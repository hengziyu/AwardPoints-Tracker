@echo off
setlocal

REM --- Force change to the script's directory to ensure all paths are correct ---
cd /d "%~dp0"

REM Set JAVA_HOME to our bundled JDK
set "JAVA_HOME=%cd%\jdk"

REM Add the bundled JDK's bin to the PATH
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Define the path to the JAR file
set "JAR_FILE=target\untitled-1.0-SNAPSHOT-jar-with-dependencies.jar"

REM Check if the JAR file exists
if not exist "%JAR_FILE%" (
    echo.
    echo Jar file not found in target directory.
    echo Please build the project first by running the command:
    echo mvn clean package
    echo.
    pause
    exit /b 1
)

REM Run the application
echo Starting the application...
java -jar "%JAR_FILE%"

endlocal
pause

