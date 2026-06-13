$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$workspaceRoot = Split-Path -Parent $projectRoot
$toolsRoot = Join-Path $workspaceRoot ".tools"
$sdkRoot = Join-Path $workspaceRoot "android-sdk"
$jdkRoot = Get-ChildItem -Path (Join-Path $toolsRoot "jdk") -Directory | Select-Object -First 1
$gradleBin = Join-Path $toolsRoot "gradle\gradle-8.9\bin\gradle.bat"

if (-not $jdkRoot) {
    throw "JDK was not found in $toolsRoot\jdk"
}

if (-not (Test-Path $gradleBin)) {
    throw "Gradle was not found at $gradleBin"
}

$env:JAVA_HOME = $jdkRoot.FullName
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:GRADLE_USER_HOME = Join-Path $workspaceRoot ".gradle-cache"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"

& $gradleBin -p $projectRoot --no-daemon assembleDebug

