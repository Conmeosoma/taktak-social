param(
    [switch]$CurlOnly
)

$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'Use-TaktakJava.ps1') -Target Server
if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) {
    throw 'curl.exe is required. Install curl or use a Windows environment that includes it.'
}

Push-Location (Join-Path $taskRoot 'server')
try {
    $taskMavenArguments = @("-Dmaven.repo.local=$taskRoot/.tools/maven-repository", '-B', '-ntp')
    if ($CurlOnly) {
        $taskMavenArguments += '-Dtest=AuthenticationIntegrationTest#curlExercisesAuthLifecycleIncludingExpiryForgeryAndDuplicateUsername', 'test'
    } else {
        $taskMavenArguments += 'verify'
    }
    & './mvnw.cmd' @taskMavenArguments
    if ($LASTEXITCODE -ne 0) { throw 'Authentication API verification failed. Inspect local Surefire reports.' }
} finally {
    Pop-Location
}
