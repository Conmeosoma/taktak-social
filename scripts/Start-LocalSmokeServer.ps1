param(
    [ValidateRange(0, 65535)]
    [int]$Port = 0,
    [switch]$EnableFcm,
    [string]$FirebaseCredentials
)

$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'Use-TaktakJava.ps1') -Target Server
Push-Location (Join-Path $taskRoot 'server')
try {
    & './mvnw.cmd' "-Dmaven.repo.local=$taskRoot/.tools/maven-repository" -B -ntp test-compile dependency:build-classpath '-Dmdep.outputFile=target/test-classpath.txt' '-DincludeScope=test'
    if ($LASTEXITCODE -ne 0) { throw 'Failed to compile the local smoke runner.' }
    $taskClassPath = (Get-Content 'target/test-classpath.txt' -Raw).Trim() + [IO.Path]::PathSeparator + 'target/classes' + [IO.Path]::PathSeparator + 'target/test-classes'
    $taskJavaArguments = @("-Dtaktak.smoke.port=$Port")
    if ($EnableFcm) {
        if (-not $FirebaseCredentials -or -not (Test-Path -LiteralPath $FirebaseCredentials -PathType Leaf)) {
            throw 'Real FCM smoke requires a local Admin credentials file.'
        }
        $taskCredentialsPath = (Resolve-Path -LiteralPath $FirebaseCredentials).Path
        $taskJavaArguments += '-Dtaktak.fcm.enabled=true'
        $taskJavaArguments += "-Dtaktak.fcm.credentials-path=$taskCredentialsPath"
    }
    & (Join-Path $env:JAVA_HOME 'bin/java.exe') @taskJavaArguments -cp $taskClassPath 'com.taktak.server.LocalSmokeServer'
    if ($LASTEXITCODE -ne 0) { throw 'Local smoke server failed.' }
} finally {
    Pop-Location
}
