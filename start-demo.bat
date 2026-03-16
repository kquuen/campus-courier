@echo off
echo ============================================
echo 校园快递代取系统 - 本地演示启动脚本
echo ============================================
echo.

REM 检查Redis是否运行
echo 检查Redis服务状态...
tasklist | findstr redis-server >nul
if %errorlevel% equ 0 (
    echo ✅ Redis服务正在运行
) else (
    echo ⚠️ Redis服务未运行，正在启动...
    start "" "E:\Redis\redis-server.exe" --daemonize yes
    timeout /t 3 /nobreak >nul
    echo ✅ Redis服务已启动
)

echo.

REM 启动后端服务
echo 启动校园快递代取系统后端服务...
echo 服务将在后台启动，端口：8080
echo.

REM 检查是否已有服务在运行
netstat -ano | findstr :8080 >nul
if %errorlevel% equ 0 (
    echo ⚠️ 端口8080已被占用，请先停止其他服务
    echo 按任意键退出...
    pause >nul
    exit /b 1
)

REM 启动后端服务
start "校园快递系统" java -jar backend\target\courier-1.0.0.jar

echo ✅ 后端服务启动中，请等待5-10秒...
timeout /t 8 /nobreak >nul

echo.
echo ============================================
echo 🎉 系统启动完成！
echo ============================================
echo.
echo 🔗 访问地址：
echo 1. 系统演示界面：file:///%CD%\test-system.html
echo 2. H2数据库控制台：http://localhost:8080/h2-console
echo 3. API接口文档：查看 test-api.http 文件
echo.
echo 👥 测试账号：
echo   普通用户：13800138000 / user123
echo   代取员：13900139000 / courier123
echo   管理员：admin / admin123
echo.
echo 📋 预置数据：
echo   - 3个用户账号
echo   - 3个订单（不同状态）
echo   - 支付和评价记录
echo.
echo ⚠️ 注意事项：
echo 1. 这是H2内存数据库，重启后数据会丢失
echo 2. 如需持久化数据，请配置MySQL
echo 3. 演示结束后按Ctrl+C停止服务
echo.
echo 按任意键打开演示界面...
pause >nul

REM 打开演示界面
start "" "test-system.html"

echo.
echo 演示界面已打开，系统正在运行中...
echo 按任意键查看部署文档...
pause >nul

REM 打开部署文档
start "" "DEMO.md"

echo.
echo ============================================
echo 系统运行中...
echo 要停止服务，请关闭Java控制台窗口
echo ============================================
echo.
pause