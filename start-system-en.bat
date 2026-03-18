@echo off
title Campus Courier System - Startup Script
color 0A

echo ============================================
echo    Campus Courier System - One-Click Startup
echo ============================================
echo.

REM Set environment variables and paths
set PROJECT_ROOT=%~dp0
set BACKEND_DIR=%PROJECT_ROOT%backend
set RESOURCES_DIR=%BACKEND_DIR%\src\main\resources

REM Check Java environment
echo [1/6] Checking Java environment...
java -version 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Java not found. Please install JDK 21+
    pause
    exit /b 1
)
echo ✓ Java environment OK
echo.

REM Check Maven environment
echo [2/6] Checking Maven environment...
mvn -version 2>nul
if %errorlevel% neq 0 (
    echo WARNING: Maven not found, trying to run JAR directly
    set USE_MAVEN=0
) else (
    echo ✓ Maven environment OK
    set USE_MAVEN=1
)
echo.

REM Check MySQL service
echo [3/6] Checking MySQL service...
sc query mysql | findstr /C:"RUNNING" >nul
if %errorlevel% neq 0 (
    echo WARNING: MySQL service not running, trying to start...
    net start mysql 2>nul
    if %errorlevel% neq 0 (
        echo ERROR: Cannot start MySQL service
        echo Please run manually: net start mysql
        pause
        exit /b 1
    )
    timeout /t 3 /nobreak >nul
    echo ✓ MySQL service started
) else (
    echo ✓ MySQL service is running
)
echo.

REM Check Memurai (Redis for Windows) service
echo [4/6] Checking Memurai (Redis compatible) service...
netstat -ano | findstr :6379 | findstr LISTENING >nul
if %errorlevel% neq 0 (
    echo WARNING: Memurai service not running or port 6379 not listening
    echo Cache functionality will be unavailable
    echo Press any key to continue (cache features may not work)...
    pause >nul
    echo WARNING: Memurai service not running, cache unavailable
) else (
    echo ✓ Memurai service is running (port 6379 is listening)
)
echo.

REM Initialize database (if not exists)
echo [5/6] Checking database status...
mysql -u root -p123456 -e "USE campus_courier;" 2>nul
if %errorlevel% neq 0 (
    echo Database does not exist, initializing...
    echo Executing database initialization script...

    REM Try with default password
    set MYSQL_PASSWORD=123456

    :retry_password
    mysql -u root -p%MYSQL_PASSWORD% < "%RESOURCES_DIR%\init-mysql.sql" 2>nul
    if %errorlevel% neq 0 (
        echo Database connection failed, please enter MySQL root password:
        set /p MYSQL_PASSWORD=Enter password (press Enter for default 123456):
        if "%MYSQL_PASSWORD%"=="" set MYSQL_PASSWORD=123456
        goto retry_password
    )

    echo ✓ Database initialized
) else (
    echo ✓ Database exists
)
echo.

REM Start backend service
echo [6/6] Starting backend service...
echo Service will run at: http://localhost:8082
echo Press Ctrl+C to stop service
echo.

cd /d "%BACKEND_DIR%"

REM Check if service is already running
netstat -ano | findstr :8082 >nul
if %errorlevel% equ 0 (
    echo WARNING: Port 8082 is already in use, trying to kill existing process...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8082 ^| findstr LISTENING') do (
        taskkill /PID %%a /F >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
)

REM Select startup method
if "%USE_MAVEN%"=="1" (
    echo Starting Spring Boot application with Maven...
    echo.
    mvn spring-boot:run
) else (
    REM Try to find and run JAR file
    if exist "target\courier-1.0.0.jar" (
        echo Starting application with JAR file...
        echo.
        java -jar target/courier-1.0.0.jar
    ) else (
        echo ERROR: JAR file not found and Maven not available
        echo Please compile the project first with Maven:
        echo   cd backend
        echo   mvn clean package
        pause
        exit /b 1
    )
)

REM If user pressed Ctrl+C to stop service
echo.
echo Service stopped
echo Press any key to exit...
pause >nul
exit /b 0