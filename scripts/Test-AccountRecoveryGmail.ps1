param(
    [Parameter(Mandatory=$true)][string]$ConfigurationPath,
    [ValidateSet('Connection','Acceptance')][string]$Mode='Connection',
    [string]$Recipient
)

$ErrorActionPreference='Stop'
$taskRoot=Split-Path -Parent $PSScriptRoot
$smtpFile=(Resolve-Path -LiteralPath $ConfigurationPath).Path
# Không dot-source/evaluate file do người dùng cung cấp; chỉ đọc năm khóa được phép.
& git -C $taskRoot check-ignore --quiet -- $smtpFile
if($LASTEXITCODE -ne 0){ throw 'SMTP file must be Git-ignored before testing.' }
$smtpNames=@('MAIL_HOST','MAIL_PORT','MAIL_USERNAME','MAIL_PASSWORD','MAIL_FROM')
$originalSettings=@{}
foreach($name in $smtpNames+@('TAKTAK_QA_EMAIL')){ $originalSettings[$name]=[Environment]::GetEnvironmentVariable($name) }
try {
    foreach($line in [IO.File]::ReadAllLines($smtpFile,[Text.Encoding]::UTF8)){
        if([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')){ continue }
        # File .txt có thể chứa tiêu đề/ghi chú; không thực thi và không in dòng ngoài cấu hình.
        if($line -notmatch '^\s*(MAIL_[A-Z_]+)\s*=\s*(.*?)\s*$'){ continue }
        $name=$Matches[1]; $value=$Matches[2]
        if($smtpNames -notcontains $name){ throw 'Unexpected SMTP setting name.' }
        if($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))){
            $value=$value.Substring(1,$value.Length-2)
        }
        [Environment]::SetEnvironmentVariable($name,$value,'Process')
    }
    foreach($name in $smtpNames){ if([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))){ throw 'Missing required SMTP setting.' } }
    if($env:MAIL_HOST -eq 'smtp.gmail.com' -and $env:MAIL_PASSWORD -match '^[a-z ]{16,24}$'){ $env:MAIL_PASSWORD=$env:MAIL_PASSWORD.Replace(' ','') }
    if($Mode -eq 'Acceptance' -and [string]::IsNullOrWhiteSpace($Recipient)){ throw 'Explicit recipient approval is required for real emails.' }
    $env:TAKTAK_QA_EMAIL=$Recipient
    . (Join-Path $PSScriptRoot 'Use-TaktakJava.ps1') -Target Server
    $ErrorActionPreference='Continue'
    $method=if($Mode -eq 'Connection'){'smtpConnectionOnly'}else{'realSmtpAcceptsVerificationTemporaryAndChangedNotification'}
    $log=Join-Path $taskRoot ('.local/artifacts/recovery-gmail-'+$Mode.ToLowerInvariant()+'.log')
    & (Join-Path $taskRoot '.tools/apache-maven-3.9.16/bin/mvn.cmd') -f (Join-Path $taskRoot 'server/pom.xml') `
        "-Dmaven.repo.local=$taskRoot/.tools/maven-repository" -B -ntp "-Dtest=GmailRecoveryAcceptanceIT#$method" test *> $log
    $result=$LASTEXITCODE
    Get-Content -LiteralPath $log -Tail 22
    exit $result
} finally {
    foreach($name in $originalSettings.Keys){ [Environment]::SetEnvironmentVariable($name,$originalSettings[$name],'Process') }
    $value=$null
}
