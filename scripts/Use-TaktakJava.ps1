param(
    [ValidateSet('Server', 'Android')]
    [string]$Target = 'Server'
)

$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskJavaHome = $env:JAVA_HOME
if ($Target -eq 'Server') {
    $taskLocalJdk = Get-ChildItem -LiteralPath (Join-Path $taskRoot '.tools') -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like 'jdk-17*' } | Sort-Object Name -Descending | Select-Object -First 1
    if ($taskLocalJdk) { $taskJavaHome = $taskLocalJdk.FullName }
} else {
    $taskStudioJdk = Join-Path $env:ProgramFiles 'Android/Android Studio/jbr'
    if (Test-Path -LiteralPath (Join-Path $taskStudioJdk 'bin/java.exe')) { $taskJavaHome = $taskStudioJdk }
}
if (-not $taskJavaHome -or -not (Test-Path -LiteralPath (Join-Path $taskJavaHome 'bin/java.exe'))) {
    throw 'Set JAVA_HOME to the required JDK, or prepare the local .tools JDK first.'
}
$taskVersion = (& (Join-Path $taskJavaHome 'bin/java.exe') --version 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0 -or ($Target -eq 'Server' -and $taskVersion -notmatch '(?m)^(?:openjdk|java) 17\.')) {
    throw 'Server requires JDK 17; the selected Java could not be verified.'
}
$env:JAVA_HOME = $taskJavaHome
$env:PATH = (Join-Path $taskJavaHome 'bin') + [IO.Path]::PathSeparator + $env:PATH
Write-Output "Selected $Target Java for this PowerShell process only."
