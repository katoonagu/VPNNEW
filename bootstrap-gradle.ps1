param()
$ErrorActionPreference = "Stop"

$gradleVersion = "8.9"
$installRoot = Join-Path $env:USERPROFILE "gradle"
$gradleHome = Join-Path $installRoot "gradle-$gradleVersion"
$zipPath = Join-Path $installRoot "gradle-$gradleVersion-bin.zip"
$distributionUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"

if (-not (Test-Path $installRoot)) {
    New-Item -ItemType Directory -Path $installRoot | Out-Null
}

if (-not (Test-Path $gradleHome)) {
    if (-not (Test-Path $zipPath)) {
        Write-Host "Downloading Gradle $gradleVersion ..."
        Invoke-WebRequest -Uri $distributionUrl -OutFile $zipPath
    } else {
        Write-Host "Using cached archive $zipPath"
    }

    Write-Host "Extracting Gradle to $installRoot ..."
    Expand-Archive -Path $zipPath -DestinationPath $installRoot -Force
}

$gradleExe = Join-Path $gradleHome "bin/gradle.bat"
if (-not (Test-Path $gradleExe)) {
    throw "Gradle executable not found at $gradleExe"
}

Write-Host "Generating Gradle wrapper ($gradleVersion) ..."
& $gradleExe "wrapper" "--gradle-version" $gradleVersion
