@echo off
title Campus Courier System - Simple Startup
color 0A

echo ============================================
echo    Campus Courier System - Startup Script
echo ============================================
echo.

set PROJECT_ROOT=%~dp0
set BACKEND_DIR=%PROJECT_ROOT%backend

echo [1] Checking Java...
java -version 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Java not found. Install JDK 21+
    pause
    exit /b 1
)
echo OK - Java found
echo.

echo [2] Checking MySQL...
net start mysql 2>nul
timeout /t 2 /nobreak >nul
echo OK - MySQL started or already running
echo.

echo [3] Checking Redis/Memurai (port 6379)...
netstat -ano | findstr :6379 | findstr LISTENING >nul
if %errorlevel% neq 0 (
    echo WARNING: Port 6379 not listening (Redis/Memurai not running)
    echo Cache features will not work
)
echo.

echo [4] Starting backend service...
echo Service at: http://localhost:8082
echo Press Ctrl+C to stop
echo.

cd /d "%BACKEND_DIR%"

netstat -ano | findstr :8082 >nul
if %errorlevel% equ 0 (
    echo Port 8082 in use, cleaning up...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8082 ^| findstr LISTENING') do (
        taskkill /PID %%a /F >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
)

REM Try Maven first, then JAR
mvn -version 2>nul
if %errorlevel% equ 0 (
    echo Starting with Maven...
    mvn spring-boot:run
) else (
    if exist "target\courier-1.0.0.jar" (
        echo Starting with JAR...
        java -jar target/courier-1.0.0.jar
    ) else (
        echo ERROR: No JAR file found and Maven not available
        echo Compile first: cd backend && mvn clean package
        pause
    )
)

echo.
echo Service stopped. Press any key...
pause >nul