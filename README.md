# 校园快递代取系统

基于 Android + Spring Boot 的校园快递代取平台，毕业设计项目。

## 项目结构

```
campus-courier/
├── backend/          # Spring Boot 后端
│   ├── src/main/
│   │   ├── java/com/campus/courier/
│   │   │   ├── controller/   # REST 接口层
│   │   │   ├── service/      # 业务逻辑层
│   │   │   ├── mapper/       # MyBatis-Plus 数据层
│   │   │   ├── entity/       # 数据库实体
│   │   │   ├── config/       # JWT拦截器、WebConfig
│   │   │   └── util/         # JWT工具类
│   │   └── resources/
│   │       ├── application.yml
│   │       └── init.sql      # 数据库初始化脚本
│   └── pom.xml
└── frontend-android/ # Android 客户端
    └── app/src/main/
        ├── java/com/campus/courier/
        │   ├── activity/     # 所有页面
        │   ├── adapter/      # RecyclerView 适配器
        │   └── api/          # 网络请求封装
        └── res/              # 布局、菜单、资源
```

## 技术栈

| 层级 | 技术 |
|------|------|
| Android 客户端 | Java、OkHttp、Gson、Material Design |
| 后端框架 | Spring Boot 3.3 + Java 21 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| ORM | MyBatis-Plus |
| 认证 | JWT |

## 核心功能

- **用户模块**：注册/登录（手机号）、申请成为代取员、信用分体系
- **订单模块**：发布代取需求、代取员接单、取件状态流转、实时跟踪
- **支付模块**：模拟微信/支付宝/余额三种支付方式
- **评价模块**：订单完成后双向评价，加权算法更新信用分
- **管理模块**：用户管理（禁用/启用）、订单监控

## 🚀 快速部署演示

### 方法一：一键启动（推荐）
1. 双击运行 `start-demo.bat`
2. 脚本会自动启动Redis和后端服务
3. 打开演示界面 `test-system.html`

### 方法二：手动启动
1. 确保Redis服务运行（默认在E:\Redis）
2. 启动后端服务：
   ```bash
   cd backend
   java -jar target/courier-1.0.0.jar
   ```
3. 访问演示界面：`test-system.html`

### 演示配置
- **数据库**：H2内存数据库（无需安装MySQL）
- **端口**：8080
- **测试账号**：
  - 普通用户：13800138000 / user123
  - 代取员：13900139000 / courier123
  - 管理员：admin / admin123

## 📁 新增文件说明

| 文件 | 说明 |
|------|------|
| `DEMO.md` | 完整的部署和演示文档 |
| `test-system.html` | 可视化演示界面 |
| `start-demo.bat` | 一键启动脚本 |
| `test-api.http` | API测试脚本 |
| `init-mysql.bat` | MySQL初始化脚本 |

## 本地启动步骤（生产环境）

### 后端

1. 安装 JDK 21、MySQL 8.0、Redis
2. 创建数据库并执行初始化脚本：
   ```bash
   mysql -u root -p < backend/src/main/resources/init.sql
   ```
3. 修改 `application.yml` 中的数据库密码
4. 启动：
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   服务运行在 `http://localhost:8080`

### Android 客户端

1. 用 Android Studio 打开 `frontend-android/` 目录
2. 如使用真机调试，将 `ApiClient.java` 中的 `10.0.2.2` 改为电脑局域网 IP
3. 点击 Run 编译安装到模拟器或真机

## 主要接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/user/register | 注册 |
| POST | /api/user/login | 登录 |
| POST | /api/order/publish | 发布代取需求 |
| GET  | /api/order/pending | 查看待接单列表 |
| POST | /api/order/{id}/accept | 接单 |
| POST | /api/order/{id}/complete | 完成订单 |
| POST | /api/payment/pay | 发起支付 |
| POST | /api/review/submit | 提交评价 |

## 订单状态流转

```
待接单(0) → 已接单(1) → 取件中(2) → 已完成(3)
                                  ↘ 已取消(4)
```
