# FamilyTutorHub

家教管理系统，包含后端服务、Web 管理后台和 Android App。

## 项目结构

```
├── tutor-backend/   # Go 后端服务
├── tutor-admin/     # React 管理后台
└── tutor-app/       # Android 客户端
```

## 快速开始

### 使用 Docker Compose（推荐）

```bash
# 启动所有服务
docker-compose up -d

# 仅启动后端
docker-compose up -d tutor-backend
```

### 手动启动

#### 后端服务

```bash
cd tutor-backend

# 复制配置
cp config/config.example.yaml config/config.yaml

# 生成 Swagger 文档（首次或接口变更后）
swag init

# 运行
go run main.go
```

#### 管理后台

```bash
cd tutor-admin
pnpm install
pnpm dev
```

## API 文档

后端启动后访问 Swagger UI：

```
http://localhost:8080/swagger/index.html
```

也可导出 `tutor-backend/docs/swagger.json` 到 Apifox 等工具。

## 默认账号

- 用户名：`admin`
- 密码：`admin123`

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Go, Gin, GORM, SQLite/MySQL |
| 前端 | React, Ant Design Pro, TypeScript |
| App | Kotlin, Jetpack Compose |
