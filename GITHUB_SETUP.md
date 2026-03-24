# GitHub仓库设置指南

## 当前状态
- ✅ 项目已初始化为git仓库
- ✅ 所有文件已提交（2次提交）
- ✅ Git用户名配置：kquuen
- ✅ Git邮箱配置：2356250854@qq.com

## 提交记录
1. **abd93a0** - 部署完成：校园快递代取系统本地部署
   - 添加MySQL数据库配置
   - 创建演示文档和界面
   - 添加一键启动脚本
   - 完善测试数据和API文档
   - 优化部署配置

2. **2876ebd** - 更新README.md：添加快速部署演示说明和新增文件文档

## 推送到GitHub的步骤

### 选项一：创建新仓库（推荐）
1. **创建GitHub仓库**：
   - 访问 https://github.com/new
   - 仓库名称：`campus-courier`
   - 描述：校园快递代取系统 - 毕业设计项目
   - 选择公开（Public）或私有（Private）
   - **重要**：不要初始化README、.gitignore或license
   - 点击"Create repository"

2. **获取仓库URL**：
   - 创建后复制仓库URL，格式为：
     ```
     https://github.com/kquuen/campus-courier.git
     ```

3. **运行推送脚本**：
   - 双击运行 `push-to-github.bat`
   - 选择选项1（创建新仓库）
   - 粘贴仓库URL
   - 等待推送完成

### 选项二：推送到现有仓库
1. **获取现有仓库URL**：
   - 格式：`https://github.com/kquuen/仓库名称.git`

2. **运行推送脚本**：
   - 双击运行 `push-to-github.bat`
   - 选择选项2（现有仓库）
   - 粘贴仓库URL
   - 等待推送完成

## 验证推送成功
1. 访问你的GitHub主页：https://github.com/kquuen
2. 查看仓库列表，应该能看到 `campus-courier` 仓库
3. 点击进入仓库，查看代码文件

## 后续操作建议

### 1. 添加.gitignore（可选）
如果需要忽略某些文件，创建 `.gitignore` 文件：
```
# 编译输出
target/
build/
*.class
*.jar
*.war

# 日志文件
logs/
*.log

# 开发环境文件
.env
*.iml
.idea/
*.swp
*.swo

# 系统文件
.DS_Store
Thumbs.db
```

### 2. 添加许可证（可选）
为项目添加开源许可证，如MIT License。

### 3. 设置GitHub Pages（可选）
如果需要在线演示，可以启用GitHub Pages：
1. 仓库设置 → Pages
2. 选择分支（main）
3. 选择文件夹（/docs 或 /）
4. 保存

## 故障排除

### 问题1：推送被拒绝
**原因**：仓库已存在且有不同历史
**解决**：使用 `--force` 推送（已在脚本中包含）

### 问题2：认证失败
**原因**：需要GitHub访问令牌
**解决**：
1. 访问 https://github.com/settings/tokens
2. 生成新令牌（token）
3. 选择权限：repo
4. 使用令牌代替密码

### 问题3：网络连接问题
**解决**：
1. 检查网络连接
2. 尝试使用SSH方式：
   ```
   git remote set-url origin git@github.com:kquuen/campus-courier.git
   ```

## 联系信息
- GitHub用户名：kquuen
- Git邮箱：2356250854@qq.com
- 项目路径：C:\Users\YaoFeng\IdeaProjects\campus-courier

---
*文档生成时间：2026-03-15 15:22*