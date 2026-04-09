@echo off
@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Batch Script
@REM ----------------------------------------------------------------------------

setlocal EnableDelayedExpansion

@REM Set the Maven home directory
set "MAVEN_HOME=D:\java\maven\apache-maven-3.9.12"
set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

@REM Check if Maven exists
if not exist "%MAVEN_CMD%" (
    echo Error: Maven not found at %MAVEN_HOME%
    echo Please install Maven or set MAVEN_HOME correctly.
    exit /b 1
)

@REM Run Maven with all arguments
"%MAVEN_CMD%" %*

exit /b %ERRORLEVEL%
