@echo off
echo ============================================
echo 校园快递代取系统 MySQL数据库初始化
echo ============================================
echo.

REM 设置MySQL路径
set MYSQL_PATH=E:\MYSQL\mysql-9.6.0-winx64\bin
set MYSQL_EXE=%MYSQL_PATH%\mysql.exe
set SQL_FILE=backend\src\main\resources\init-mysql.sql

REM 检查MySQL是否安装
if not exist "%MYSQL_EXE%" (
    echo ❌ 错误：MySQL未找到在 %MYSQL_PATH%
    echo 请确保MySQL已正确安装在E:\MYSQL目录
    pause
    exit /b 1
)

echo ✅ MySQL找到: %MYSQL_EXE%
echo.

REM 尝试连接MySQL并创建数据库
echo 正在连接MySQL并创建数据库...
"%MYSQL_EXE%" -u root -p015018 -e "CREATE DATABASE IF NOT EXISTS campus_courier CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul

if %errorlevel% neq 0 (
    echo ❌ 错误：无法连接到MySQL
    echo 可能的原因：
    echo 1. MySQL服务未启动
    echo 2. 密码不正确
    echo 3. MySQL安装有问题
    echo.
    echo 请手动启动MySQL服务：
    echo 1. 打开服务管理器 (services.msc)
    echo 2. 找到MySQL服务并启动
    echo 3. 或运行：net start mysql
    echo.
    echo 或者使用H2内存数据库继续演示：
    echo 修改 application.yml 使用H2配置
    pause
    exit /b 1
)

echo ✅ 数据库 campus_courier 创建成功
echo.

REM 执行初始化脚本
echo 正在执行数据库初始化脚本...
"%MYSQL_EXE%" -u root -p015018 campus_courier < "%SQL_FILE%" 2>nul

if %errorlevel% neq 0 (
    echo ⚠️ 警告：执行SQL脚本时出错
    echo 但数据库已创建，应用可以启动
) else (
    echo ✅ 数据库表创建成功
)

echo.
echo ============================================
echo 数据库初始化完成！
echo ============================================
echo.
echo 下一步：
echo 1. 确保Redis服务正在运行
echo 2. 启动后端应用：
echo    cd backend
echo    java -jar target/courier-1.0.0.jar
echo.
echo 如果MySQL无法启动，可以：
echo 1. 使用H2内存数据库：修改application.yml
echo 2. 或联系系统管理员检查MySQL安装
echo.
pause