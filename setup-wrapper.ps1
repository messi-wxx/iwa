# 补全 Gradle Wrapper（误点 Skip 时可运行此脚本）
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$wrapperDir = Join-Path $root "gradle\wrapper"
New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null

$jarUrl = "https://github.com/gradle/gradle/raw/v8.10.2/gradle/wrapper/gradle-wrapper.jar"
$jarPath = Join-Path $wrapperDir "gradle-wrapper.jar"
Write-Host "Downloading gradle-wrapper.jar ..."
Invoke-WebRequest -Uri $jarUrl -OutFile $jarPath -UseBasicParsing
Write-Host "Saved: $jarPath ($((Get-Item $jarPath).Length) bytes)"

if (-not (Test-Path (Join-Path $root ".git"))) {
    Set-Location $root
    git init
    Write-Host "Git repository initialized."
}

Write-Host "Done. Open project in Android Studio or run: .\gradlew.bat assembleDebug"
