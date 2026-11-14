param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$log = Join-Path $repo '.local/artifacts/advanced4-live-usb-final.log'
New-Item -ItemType Directory -Force -Path (Split-Path $log) | Out-Null
function Invoke-Adb([string[]]$Arguments) {
    $result = & $Adb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "ADB failed: $($Arguments[0])" }
    return ($result -join "`n")
}
# Chỉ chạy sau khi người dùng cho phép chụp/quay QA vào cache.
# Không clear data, đổi tài khoản hoặc gửi request tới production.
Invoke-Adb -Arguments @('get-state') | Out-Null
$packageState = Invoke-Adb -Arguments @('shell','dumpsys','package','com.taktak.app')
$temporary = @()
try {
    foreach ($permission in @('android.permission.CAMERA','android.permission.RECORD_AUDIO')) {
        $pattern = [regex]::Escape($permission) + ': granted=true'
        if ($packageState -notmatch $pattern) {
            Invoke-Adb -Arguments @('shell','pm','grant','com.taktak.app',$permission) | Out-Null
            $temporary += $permission
        }
    }
    $result = Invoke-Adb -Arguments @('shell','am','instrument','-w','-r','-e','class',
        'com.taktak.app.ui.theme.AdvancedFeaturesDeviceQaTest',
        'com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner')
    $result | Set-Content -Encoding utf8 -LiteralPath $log
    if ($result -notmatch 'OK \(8 tests\)' -or $result -match 'Process crashed|FAILURES!!!') {
        throw "USB acceptance failed. See $log"
    }
    Write-Output "PASS: 8 USB tests. Log: $log"
} finally {
    # Thu hồi sau khi runner kết thúc vì Android sẽ dừng process khi revoke.
    foreach ($permission in $temporary) {
        Invoke-Adb -Arguments @('shell','pm','revoke','com.taktak.app',$permission) | Out-Null
    }
}
