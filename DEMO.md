# 校园快递代取系统 - 本地部署演示

## 🚀 部署完成！

恭喜！校园快递代取系统已在本地成功部署并运行。

## 📊 系统状态

### ✅ 已启动的服务
1. **后端服务**：Spring Boot 3.3 + Java 21
   - 端口：8080
   - 状态：✅ 运行中
   - 启动时间：3.034秒

2. **数据库**：MySQL 8.0
   - 类型：关系型数据库
   - 连接URL：`jdbc:mysql://localhost:3306/campus_courier`
   - 用户名：`root`
   - 密码：`123456`

3. **Redis/Memurai**：本地缓存服务
   - 状态：✅ 运行中（端口6379）
   - 说明：Windows用户使用Memurai作为Redis兼容服务

## 🔗 访问地址


### Web接口
- **后端API**：http://localhost:8080
- **Redis/Memurai监控**：需使用Redis客户端连接 localhost:6379（Windows使用Memurai）
- **健康检查**：http://localhost:8080/actuator/health

### 测试账号
系统已预置以下测试账号：

| 角色 | 手机号 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 系统管理员 |
| 普通用户 | 13800138000 | user123 | 普通用户，可发布订单 |
| 代取员 | 13900139000 | courier123 | 代取员，可接单 |

## 📋 预置数据

### 数据库表
1. **用户表** (user) - 3条记录
2. **订单表** (order) - 3条记录（不同状态）
3. **支付记录表** (payment) - 1条记录
4. **评价表** (review) - 2条记录
5. **提现记录表** (withdrawal) - 空表

### 订单状态示例
1. **待接单** - 订单号：CO202403150001
2. **已接单** - 订单号：CO202403150002
3. **取件中** - 订单号：CO202403150003

## 🛠️ API测试

### 1. 用户登录
```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "phone": "13800138000",
  "password": "user123"
}
```

### 2. 获取用户信息
```bash
GET http://localhost:8080/api/user/info
Authorization: Bearer {token}
```

### 3. 获取订单列表
```bash
GET http://localhost:8080/api/order/list?page=1&size=10
Authorization: Bearer {token}
```

## 🎯 核心功能演示

### 普通用户流程
1. 登录 → 2. 发布订单 → 3. 查看订单状态 → 4. 支付 → 5. 评价

### 代取员流程
1. 登录 → 2. 查看待接订单 → 3. 接单 → 4. 确认取件 → 5. 完成订单

## 🔧 技术栈

### 后端技术
- **框架**：Spring Boot 3.3.0
- **Java版本**：21
- **数据库**：MySQL 8.0
- **缓存**：Redis
- **ORM**：MyBatis Plus
- **安全**：JWT认证
- **构建工具**：Maven

### 部署特点
1. **MySQL数据库**：需安装MySQL 8.0
2. **Redis缓存**：需安装Redis
3. **预置测试数据**：开箱即用
4. **完整日志**：便于调试

## 📁 项目结构
```
campus-courier/
├── backend/                    # 后端项目
│   ├── src/main/java/         # Java源代码
│   ├── src/main/resources/    # 配置文件
│   │   ├── application.yml    # 主配置
│   │   ├── application-demo.yml # 演示配置
│   │   ├── init-mysql.sql     # MySQL初始化脚本
│   │   └── data-mysql.sql     # MySQL测试数据
│   └── pom.xml               # Maven配置
├── DEMO.md                   # 本演示文档
└── test-api.http            # API测试脚本
```

## ⚡ 快速开始演示

### 方法一：使用启动脚本（推荐）
1. 双击运行 `start-demo.bat`
2. 脚本会自动：
   - 检查并启动Redis服务
   - 启动后端Spring Boot应用
   - 打开演示界面
   - 显示测试账号信息

### 方法二：手动启动
1. 确保Redis服务运行：
   ```bash
   # Redis默认在E:\Redis\redis-server.exe
   # 或运行：start "" "E:\Redis\redis-server.exe"
   ```

2. 启动后端服务：
   ```bash
   cd C:\Users\YaoFeng\IdeaProjects\campus-courier\backend
   java -jar target/courier-1.0.0.jar
   ```

3. 访问演示界面：
   - 打开 `test-system.html` 文件
   - 或直接访问：http://localhost:8080

### 验证服务运行
```bash
# 检查服务是否响应
curl http://localhost:8080

# 访问MySQL数据库
# 使用MySQL客户端连接：
# - Host: localhost:3306
# - Database: campus_courier
# - User Name: root
# - Password: 123456
```

## 🎓 毕业设计演示要点

### 功能演示
1. **用户管理**：注册、登录、个人信息
2. **订单管理**：发布、接单、状态流转
3. **支付系统**：模拟支付流程
4. **评价系统**：双向评价机制
5. **信用体系**：用户信用分管理

### 技术亮点
1. **微服务架构**：Spring Boot + Redis
2. **安全认证**：JWT + Spring Security
3. **数据库设计**：多表关联，事务管理
4. **API设计**：RESTful风格，统一响应格式
5. **错误处理**：全局异常处理，友好错误提示

## 🔄 使用MySQL数据库（推荐）

当前已配置使用MySQL数据库，密码已设置为：`015018`

### 初始化MySQL数据库：
1. 运行初始化脚本：
   ```bash
   # 双击运行 init-mysql.bat
   # 或手动执行：
   cd C:\Users\YaoFeng\IdeaProjects\campus-courier
   init-mysql.bat
   ```

2. 如果MySQL服务未启动，请手动启动：
   ```bash
   # 方法1：服务管理器
   services.msc  # 找到MySQL服务并启动
   
   # 方法2：命令行
   net start mysql
   ```

3. 启动应用：
   ```bash
   cd backend
   java -jar target/courier-1.0.0.jar
   ```


## 📞 技术支持

如有问题，请检查：
1. 端口8080是否被占用
2. Redis服务是否正常运行
3. Java版本是否为21+

---

**部署完成时间**：2026-03-15 15:04:30
**系统运行状态**：✅ 正常