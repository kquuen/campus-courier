# 校园快递代取系统 - PowerShell推送脚本
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "校园快递代取系统 - GitHub推送" -ForegroundColor Cyan
Write-Host "仓库: https://github.com/kquuen/campus-courier.git" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# 检查当前目录
$currentDir = Get-Location
Write-Host "当前目录: $currentDir" -ForegroundColor Yellow

# 检查git状态
Write-Host "`n步骤1: 检查git状态..." -ForegroundColor Green
git status

# 显示提交记录
Write-Host "`n步骤2: 提交记录..." -ForegroundColor Green
git log --oneline -5

Write-Host "`n步骤3: 准备推送到GitHub" -ForegroundColor Green
Write-Host "--------------------------------------------" -ForegroundColor Gray

Write-Host "`n📋 推送信息：" -ForegroundColor Yellow
Write-Host "• 远程仓库: https://github.com/kquuen/campus-courier.git" -ForegroundColor White
Write-Host "• 分支: main" -ForegroundColor White
Write-Host "• 提交数: 3" -ForegroundColor White
Write-Host "• 文件数: 71" -ForegroundColor White

Write-Host "`n🔑 认证说明：" -ForegroundColor Yellow
Write-Host "如果提示输入凭据：" -ForegroundColor White
Write-Host "1. 用户名: kquuen" -ForegroundColor White
Write-Host "2. 密码: 使用GitHub访问令牌" -ForegroundColor White
Write-Host "   (不是登录密码，需要从 https://github.com/settings/tokens 生成)" -ForegroundColor White

Write-Host "`n🚀 开始推送..." -ForegroundColor Green
Write-Host "--------------------------------------------" -ForegroundColor Gray

# 尝试推送
try {
    $pushOutput = git push -u origin main 2>&1
    Write-Host $pushOutput -ForegroundColor Gray
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n✅ 推送成功！" -ForegroundColor Green
        Write-Host "`n🌐 访问你的仓库：" -ForegroundColor Cyan
        Write-Host "https://github.com/kquuen/campus-courier" -ForegroundColor Blue
        Write-Host "`n📊 包含内容：" -ForegroundColor Cyan
        Write-Host "• 完整的Spring Boot后端" -ForegroundColor White
        Write-Host "• Android客户端" -ForegroundColor White
        Write-Host "• 演示文档和界面" -ForegroundColor White
        Write-Host "• 一键启动脚本" -ForegroundColor White
        Write-Host "• API测试工具" -ForegroundColor White
    } else {
        Write-Host "`n❌ 推送失败 (错误码: $LASTEXITCODE)" -ForegroundColor Red
        
        Write-Host "`n🔧 故障排除：" -ForegroundColor Yellow
        Write-Host "1. 检查网络连接" -ForegroundColor White
        Write-Host "2. 验证GitHub访问令牌" -ForegroundColor White
        Write-Host "3. 尝试使用GitHub Desktop客户端" -ForegroundColor White
        Write-Host "4. 或稍后重试" -ForegroundColor White
        
        Write-Host "`n💡 备选方案：" -ForegroundColor Yellow
        Write-Host "• 使用GitHub Desktop: https://desktop.github.com/" -ForegroundColor White
        Write-Host "• 使用SSH方式（更稳定）" -ForegroundColor White
        Write-Host "• 分多次推送小文件" -ForegroundColor White
    }
}
catch {
    Write-Host "`n❌ 推送过程中出错: $_" -ForegroundColor Red
}

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "操作完成" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# 保持窗口打开
Write-Host "`n按任意键退出..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")