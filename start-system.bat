@echo off
title Campus Courier System - Startup Script
color 0A

echo ============================================
echo    Campus Courier System - Startup Script
echo ============================================
echo.Note: Windows users use Memurai as Redis compatible service
echo.      https://www.memurai.com/
echo.

REM 设置环境变量和路径
set PROJECT_ROOT=%~dp0
set BACKEND_DIR=%PROJECT_ROOT%backend
set RESOURCES_DIR=%BACKEND_DIR%\src\main\resources

REM 检查Java环境
echo [1/6] 检查Java环境...
java -version 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未检测到Java环境，请安装JDK 21+
    pause
    exit /b 1
)
echo ✓ Java环境检测通过
echo.

REM 检查Maven环境
echo [2/6] 检查Maven环境...
mvn -version 2>nul
if %errorlevel% neq 0 (
    echo 警告: 未检测到Maven，尝试直接运行JAR包
    set USE_MAVEN=0
) else (
    echo ✓ Maven环境检测通过
    set USE_MAVEN=1
)
echo.

REM 检查MySQL服务
echo [3/6] 检查MySQL服务...
sc query mysql | findstr /C:"RUNNING" >nul
if %errorlevel% neq 0 (
    echo 警告: MySQL服务未运行，正在尝试启动...
    net start mysql 2>nul
    if %errorlevel% neq 0 (
        echo 错误: 无法启动MySQL服务，请手动启动
        echo 请运行: net start mysql
        pause
        exit /b 1
    )
    timeout /t 3 /nobreak >nul
    echo ✓ MySQL服务已启动
) else (
    echo ✓ MySQL服务正在运行
)
echo.

REM 检查Memurai (Redis for Windows) 服务
echo [4/6] 检查Memurai (Redis兼容) 服务...
REM 方法1：检查端口6379是否被监听
netstat -ano | findstr :6379 | findstr LISTENING >nul
if %errorlevel% neq 0 (
    echo 警告: Memurai服务未运行，正在尝试启动...

    REM 尝试多种方式启动Memurai
    if exist "C:\Program Files\Memurai\memurai.exe" (
        echo 检测到Memurai在Program Files目录，正在启动...
        start "Memurai Server" /B "C:\Program Files\Memurai\memurai.exe"
    ) else if exist "C:\Program Files (x86)\Memurai\memurai.exe" (
        echo 检测到Memurai在Program Files (x86)目录，正在启动...
        start "Memurai Server" /B "C:\Program Files (x86)\Memurai\memurai.exe"
    ) else (
        REM 尝试作为Windows服务启动
        sc query memurai 2>nul | findstr RUNNING >nul
        if %errorlevel% neq 0 (
            echo 尝试启动Memurai服务...
            net start memurai 2>nul
            if %errorlevel% neq 0 (
                echo 警告: 无法启动Memurai服务
                echo 请手动启动Memurai或确保端口6379可用
                echo 可以跳过此步骤，但缓存功能将不可用
                set /p SKIP_REDIS=是否跳过Redis检查？(y/n, 默认y):
                if /i "%SKIP_REDIS%"=="n" (
                    pause
                    exit /b 1
                ) else (
                    echo ⚠ 跳过Redis检查，继续运行
                    goto skip_redis
                )
            )
        )
    )

    timeout /t 5 /nobreak >nul

    REM 再次检查端口是否被监听
    netstat -ano | findstr :6379 | findstr LISTENING >nul
    if %errorlevel% neq 0 (
        echo 警告: Memurai启动失败或端口6379未监听
        echo 缓存功能将不可用，但应用仍可启动
        echo 按任意键继续（应用启动后可能无法使用缓存功能）...
        pause >nul
        :skip_redis
        echo ⚠ Memurai服务未运行，缓存功能不可用
    ) else (
        echo ✓ Memurai服务已启动
    )
) else (
    echo ✓ Memurai服务正在运行 (端口6379已被监听)
)
echo.

REM 初始化数据库（如果不存在）
echo [5/6] 检查数据库状态...
mysql -u root -p123456 -e "USE campus_courier;" 2>nul
if %errorlevel% neq 0 (
    echo 数据库不存在，正在初始化...
    echo 正在执行数据库初始化脚本...

    REM 备份原密码，尝试使用默认密码
    set MYSQL_PASSWORD=123456

    :retry_password
    mysql -u root -p%MYSQL_PASSWORD% < "%RESOURCES_DIR%\init-mysql.sql" 2>nul
    if %errorlevel% neq 0 (
        echo 数据库连接失败，请手动输入MySQL root密码:
        set /p MYSQL_PASSWORD=请输入密码（直接回车使用默认密码123456）:
        if "%MYSQL_PASSWORD%"=="" set MYSQL_PASSWORD=123456
        goto retry_password
    )

    echo ✓ 数据库初始化完成
) else (
    echo ✓ 数据库已存在
)
echo.

REM 启动后端服务
echo [6/6] 启动后端服务...
echo 服务将运行在: http://localhost:8082
echo 按 Ctrl+C 停止服务
echo.

cd /d "%BACKEND_DIR%"

REM 检查是否已存在运行的服务
netstat -ano | findstr :8082 >nul
if %errorlevel% equ 0 (
    echo 警告: 端口8082已被占用，尝试终止现有进程...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8082 ^| findstr LISTENING') do (
        taskkill /PID %%a /F >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
)

REM 选择启动方式
if "%USE_MAVEN%"=="1" (
    echo 使用Maven启动Spring Boot应用...
    echo.
    mvn spring-boot:run
) else (
    REM 尝试查找并运行JAR包
    if exist "target\courier-1.0.0.jar" (
        echo 使用JAR包启动应用...
        echo.
        java -jar target/courier-1.0.0.jar
    ) else (
        echo 错误: 未找到JAR包，且Maven不可用
        echo 请先使用 Maven 编译项目:
        echo   cd backend
        echo   mvn clean package
        pause
        exit /b 1
    )
)

REM 如果用户按Ctrl+C停止服务，脚本会继续执行到这里
echo.
echo 服务已停止
echo 按任意键退出...
pause >nul
exit /b 0