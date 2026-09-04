@echo off
setlocal
cd /d "%~dp0"
if not exist "gradle\wrapper" mkdir "gradle\wrapper"
echo Downloading gradle-wrapper.jar ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://github.com/gradle/gradle/raw/v8.10.2/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar' -UseBasicParsing"
if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo gradle-wrapper.jar OK
) else (
    echo FAILED to download gradle-wrapper.jar
    exit /b 1
)
if not exist ".git" (
    git init
    echo Git initialized.
)
echo Done. Run: gradlew.bat assembleDebug
pause
