# 修复Java文件UTF-8 BOM编码问题
# 请在backend目录下执行此脚本

Write-Host "=== 开始修复UTF-8 BOM编码问题 ===" -ForegroundColor Cyan

$javaFiles = Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse
$fixedCount = 0

foreach ($file in $javaFiles) {
    try {
        # 读取文件内容
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        
        # 创建UTF-8无BOM编码
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        
        # 写回文件（无BOM）
        [System.IO.File]::WriteAllText($file.FullName, $content, $utf8NoBom)
        
        $fixedCount++
    } catch {
        Write-Host "✗ 修复失败: $($file.Name) - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n=== 已成功修复 $fixedCount 个Java文件 ===" -ForegroundColor Green
Write-Host "`n现在可以重新编译了: mvn clean compile" -ForegroundColor Yellow
