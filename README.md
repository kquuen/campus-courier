# 校园快递代取系统

基于 Android + Spring Boot 的校园快递代取平台，毕业设计项目。提供完整的用户管理、订单发布、智能匹配、支付结算、信用评价等功能，适用于校园快递代取场景。

## 📱 项目概述

校园快递代取系统是一个面向校园环境的快递代取服务平台，连接需要代取快递的学生和有时间的代取员。系统采用微服务架构，包含完整的用户认证、订单管理、支付结算和信用评价体系。

## 🏗️ 项目结构

```
campus-courier/
├── backend/          # Spring Boot 后端服务
│   ├── src/main/java/com/campus/courier/
│   │   ├── controller/   # REST API控制器层
│   │   ├── service/      # 业务逻辑层
│   │   ├── mapper/       # MyBatis-Plus数据访问层
│   │   ├── entity/       # 数据库实体类
│   │   ├── config/       # 配置类（JWT、Redis、Web等）
│   │   ├── dto/         # 数据传输对象
│   │   └── util/        # 工具类（JWT工具）
│   └── src/main/resources/
│       ├── application.yml          # 主配置文件
│       ├── application-minimal.yml  # 最小化配置
│       ├── application-with-redis.yml # Redis配置
│       ├── data-mysql.sql          # 测试数据
│       └── init-mysql.sql          # MySQL初始化脚本
└── frontend-android/ # Android客户端
    └── app/src/main/
        ├── java/com/campus/courier/
        │   ├── activity/     # 所有Activity页面
        │   ├── adapter/      # RecyclerView适配器
        │   └── api/          # 网络请求封装
        └── res/              # 布局、菜单、资源文件
```

## 🛠️ 技术栈

### 后端技术栈
- **框架**: Spring Boot 3.3 + Java 21
- **数据库**: MySQL 8.0
- **ORM**: MyBatis-Plus 3.5.7
- **缓存**: Redis + Spring Cache
- **构建工具**: Maven
- **其他**: Spring Validation、Jackson、Lombok

### Android客户端技术栈
- **语言**: Java
- **网络**: OkHttp 4.12.0
- **JSON解析**: Gson 2.10.1
- **图片加载**: Glide 4.16.0
- **UI**: Material Design、RecyclerView、SwipeRefreshLayout
- **最低SDK**: 24 (Android 7.0)

## 🚀 核心功能模块

### 1. 用户管理模块
- **用户注册**: 手机号、密码、学号、昵称注册
- **用户登录**: JWT令牌认证，支持24小时有效期
- **角色系统**:
  - 0: 普通用户（可发布订单）
  - 1: 代取员（可接单）
  - 2: 管理员（系统管理）
- **信用体系**: 初始信用分100分，根据评价动态调整
- **钱包功能**: 用户余额管理
- **实名认证**: 申请成为代取员需要实名认证

### 2. 订单管理模块
- **发布订单**: 填写快递单号、快递公司、取件地址、送达地址、备注、费用
- **订单状态流转**:
  ```
  待接单(0) → 已接单(1) → 取件中(2) → 已完成(3)
                                  ↘ 已取消(4) ↘ 异常(5)
  ```
- **智能推荐**: 基于信用分、距离等因素智能推荐订单给代取员
- **订单查询**: 待接单列表、我发布的订单、我接的订单、订单详情

### 3. 支付模块
- **支付方式**: 模拟微信支付、支付宝支付、余额支付
- **支付流程**: 创建支付→模拟回调→更新订单状态
- **支付状态**: 0待支付、1已支付、2已退款

### 4. 评价模块
- **双向评价**: 用户评价代取员，代取员评价用户
- **评分系统**: 1-5分评分
- **信用分更新**: 基于评价动态更新用户信用分

### 5. 管理模块
- **用户管理**: 管理员可查看所有用户、禁用/启用用户
- **订单监控**: 管理员可查看所有订单及状态
- **系统管理**: 系统配置管理

### 6. 缓存模块
- **Redis缓存**: 用户信息、订单详情、订单列表缓存
- **缓存策略**: 默认1小时TTL，支持手动清除

## 🚀 快速部署演示

### 方法一：一键启动（推荐）
1. 双击运行 `start-demo.bat`
2. 脚本会自动启动Redis和后端服务
3. 打开演示界面 `test-system.html`

### 方法二：手动启动
1. 确保Redis服务运行
2. 启动后端服务：
   ```bash
   cd backend
   java -jar target/courier-1.0.0.jar
   ```
3. 访问演示界面：`test-system.html`

### 演示配置
- **数据库**: MySQL 8.0（需安装MySQL和Redis）
- **端口**: 8082
- **测试账号**:
  - 普通用户: 13800138000 / user123
  - 代取员: 13900139000 / courier123
  - 管理员: admin / admin123

## 📁 项目文件说明

| 文件                 | 说明 |
|--------------------|------|
| `DEMO.md`          | 完整的部署和演示文档 |
| `test-system.html` | 可视化演示界面 |
| `start-simple.bat` | 一键启动脚本 |
| `test-api.http`    | API测试脚本 |


## 🔧 本地启动步骤（生产环境）

### 后端部署
1. 安装 JDK 21、MySQL 8.0、Redis (Windows用户安装Memurai)
2. 创建数据库并执行初始化脚本：
   ```bash
   mysql -u root -p < backend/src/main/resources/init-mysql.sql
   ```
3. 修改 `application.yml` 中的数据库连接配置
4. 启动服务：
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   服务运行在 `http://localhost:8082`

### Android客户端部署
1. 用 Android Studio 打开 `frontend-android/` 目录
2. 修改 `ApiClient.java` 中的服务器地址（如使用真机调试）
3. 点击 Run 编译安装到模拟器或真机

## 📡 API接口设计

### 用户相关 (`/api/user`)
- `POST /register` - 用户注册
- `POST /login` - 用户登录
- `POST /logout` - 用户登出
- `GET /profile` - 获取用户资料
- `POST /apply-courier` - 申请成为代取员
- `GET /list` - 管理员查询用户列表

### 订单相关 (`/api/order`)
- `POST /publish` - 发布代取需求
- `POST /{id}/accept` - 代取员接单
- `POST /{id}/pickup` - 更新为取件中
- `POST /{id}/complete` - 完成订单（上传凭证）
- `POST /{id}/cancel` - 取消订单
- `GET /pending` - 待接单列表
- `GET /recommend` - 智能推荐订单
- `GET /my-published` - 我发布的订单
- `GET /my-courier` - 我接的订单
- `GET /{id}` - 订单详情

### 支付相关 (`/api/payment`)
- `POST /pay` - 发起支付
- `POST /callback/{paymentNo}` - 模拟支付回调
- `GET /status/{orderId}` - 查询支付状态

### 评价相关 (`/api/review`)
- `POST /submit` - 提交评价
- `GET /order/{orderId}` - 查询订单评价
- `GET /user/{userId}` - 查询用户评价

## 🗄️ 数据库设计

### 核心表结构
1. **user表**: 用户信息，包含角色、信用分、余额等字段
2. **order表**: 订单信息，包含状态流转时间戳
3. **payment表**: 支付记录，支持多种支付方式
4. **review表**: 评价记录，支持双向评价
5. **withdrawal表**: 代取员提现记录

## 🔒 安全设计

1. **JWT认证**: Bearer Token认证机制
2. **密码加密**: BCrypt强哈希加密
3. **权限控制**: 基于角色的访问控制（RBAC）
4. **Token黑名单**: 支持Token失效管理
5. **输入验证**: Spring Validation参数校验

## ✨ 项目特色

1. **校园场景优化**: 针对校园环境设计的地址系统、信用体系
2. **智能匹配**: 基于多因素的订单推荐算法
3. **双向评价**: 建立完善的信用评价体系
4. **多支付方式**: 模拟主流支付方式，支持余额支付
5. **实时状态跟踪**: 订单状态实时更新，支持凭证上传
6. **管理后台**: 完整的管理员功能，支持用户和订单管理

## 📊 支持配置模式

- **完整模式**: MySQL + Redis + Spring Boot（生产环境）
- **最小化模式**: 仅基础功能，适合快速测试
- **演示模式**: 预置测试数据，一键启动

## 📞 环境要求

- **后端**: JDK 21+, MySQL 8.0+, Redis 6.0+ (Windows用户使用Memurai)
- **客户端**: Android Studio, Android SDK 24+
- **操作系统**: Windows/Linux/macOS

---

**项目作者**: 校园快递代取系统开发团队
**项目类型**: 毕业设计项目
**技术栈**: Spring Boot + Android + MySQL + Redis/Memurai
**许可证**: MIT License