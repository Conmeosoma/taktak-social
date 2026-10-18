param(
    [string]$Class = "com.taktak.app.DeviceAcceptanceTest",
    [string]$BaseUrl = "http://127.0.0.1:8080/"
)

$ErrorActionPreference = "Stop"
. "$PSScriptRoot\Use-TaktakJava.ps1" -Target Android

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
& $adb reverse tcp:8080 tcp:8080

Push-Location (Join-Path $PSScriptRoot "..\android-client")
try {
    $gradleArgs = @(
        "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME",
        "-Dorg.gradle.java.installations.auto-detect=false",
        "-PTAKTAK_DEBUG_BASE_URL=$BaseUrl",
        "connectedDebugAndroidTest",
        "-Pandroid.testInstrumentationRunnerArguments.class=$Class",
        "--console=plain"
    )
    & .\gradlew.bat @gradleArgs
} finally {
    Pop-Location
}
