param()

$ErrorActionPreference = "Stop"
$gradleVersion = "8.9"
$installRoot = Join-Path $env:USERPROFILE "gradle"
$gradleHome = Join-Path $installRoot "gradle-$gradleVersion"
$zipPath = Join-Path $installRoot "gradle-$gradleVersion-bin.zip"

if (-not (Test-Path $installRoot)) {
  New-Item -ItemType Directory -Path $installRoot | Out-Null
}

if (-not (Test-Path $gradleHome)) {
  Write-Host "Downloading Gradle $gradleVersion..."
  $uri = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
  Invoke-WebRequest -Uri $uri -OutFile $zipPath
  Write-Host "Extracting archive..."
  Expand-Archive -Path $zipPath -DestinationPath $installRoot -Force
  Remove-Item $zipPath -Force
}

$gradleExe = Join-Path $gradleHome "bin/gradle.bat"
if (-not (Test-Path $gradleExe)) {
  throw "Gradle executable not found at $gradleExe"
}

Write-Host "Generating Gradle wrapper..."
& $gradleExe wrapper --gradle-version $gradleVersion
