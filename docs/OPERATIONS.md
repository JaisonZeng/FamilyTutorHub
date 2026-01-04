# 家教管理系统 - 运维指南

## 系统概述

家教管理系统是一个包含后端API、管理前端和移动App的完整解决方案。

### 系统架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  移动App    │     │  管理前端    │     │   后端API   │
│ (Android)   │────▶│  (React)    │────▶│   (Go)      │
└─────────────┘     └─────────────┘     └─────────────┘
                                              │
                                              ▼
                                        ┌─────────────┐
                                        │   MySQL     │
                                        │  数据库     │
                                        └─────────────┘
```

### 技术栈

- **后端**: Go + Gin + GORM
- **数据库**: MySQL 8.0 / SQLite (开发环境)
- **前端**: React + Ant Design Pro
- **移动端**: Android (Kotlin/Java)
- **容器化**: Docker + Docker Compose
- **日志**: Zap (结构化日志)

## 快速开始

### 使用Docker部署 (推荐)

```bash
# 克隆项目
git clone <repository-url>
cd FamilyTutorHub

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
```

### 手动部署

详见 [DEPLOYMENT.md](./DEPLOYMENT.md)

## 日常运维任务

### 1. 服务监控

#### 健康检查

系统提供了三个监控端点:

```bash
# 基础健康检查
curl http://localhost:8080/health

# 就绪检查 (包含数据库连接检查)
curl http://localhost:8080/health/ready

# 性能指标
curl http://localhost:8080/metrics
```

#### 查看服务状态

```bash
# Docker环境
docker-compose ps

# 查看后端容器状态
docker inspect tutor-backend

# 查看资源使用
docker stats
```

### 2. 日志管理

详见 [LOGGING.md](./LOGGING.md)

#### 快速查看日志

```bash
# Docker环境 - 实时日志
docker-compose logs -f backend

# Docker环境 - 最近100行
docker-compose logs --tail=100 backend

# 本地部署 - 查看日志文件
tail -f logs/app.log

# 查看错误日志
grep -i "error" logs/app.log

# 查看特定用户的登录日志
grep "User logged in" logs/app.log | grep "username\":\"admin"
```

### 3. 数据库管理

#### 备份数据库

```bash
# Docker环境
docker-compose exec db mysqldump -u tutor -ptutor123 tutor > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker-compose exec -T db mysql -u tutor -ptutor123 tutor < backup_20260103.sql
```

详见 [BACKUP.md](./BACKUP.md)

### 4. 服务重启

```bash
# 重启所有服务
docker-compose restart

# 只重启后端
docker-compose restart backend

# 优雅重启 (先停止再启动)
docker-compose stop backend
docker-compose up -d backend
```

### 5. 更新部署

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose up -d --build

# 查看更新日志
docker-compose logs -f backend
```

## 监控指标说明

### 健康检查响应

**基础健康检查** (`/health`):
```json
{
  "status": "ok",
  "time": "2026-01-03T18:00:00+08:00"
}
```

**就绪检查** (`/health/ready`):
```json
{
  "status": "ready",
  "database": "connected",
  "uptime": "2h30m15s"
}
```

**性能指标** (`/metrics`):
```json
{
  "uptime": "2h30m15s",
  "database": {
    "open_connections": 5,
    "in_use": 2,
    "idle": 3
  }
}
```

### 关键性能指标

- **响应时间**: 正常情况下API响应时间应 < 200ms
- **慢请求**: 超过2秒的请求会被记录为慢请求
- **数据库连接**: 建议保持在10个以内
- **错误率**: 应保持在1%以下

## 告警设置建议

### 需要立即处理的告警

1. **服务不可用**: 健康检查失败
2. **数据库连接失败**: 就绪检查失败
3. **磁盘空间不足**: < 10%
4. **内存使用过高**: > 90%
5. **错误率激增**: 5分钟内错误率 > 5%

### 需要关注的告警

1. **慢请求增多**: 5分钟内慢请求 > 10个
2. **数据库连接数高**: > 20
3. **CPU使用率高**: > 80%
4. **日志文件增长过快**: 每小时 > 100MB

## 常见问题

详见 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)

### 快速诊断

1. **服务无响应**
   ```bash
   curl http://localhost:8080/health
   docker-compose logs --tail=50 backend
   ```

2. **数据库连接问题**
   ```bash
   docker-compose exec db mysql -u tutor -ptutor123 -e "SELECT 1"
   ```

3. **查看最近错误**
   ```bash
   docker-compose logs backend | grep -i "error" | tail -20
   ```

## 安全建议

1. **修改默认密码**: 生产环境必须修改数据库密码和管理员密码
2. **使用HTTPS**: 生产环境配置SSL证书
3. **限制访问**: 配置防火墙规则,只开放必要端口
4. **定期备份**: 每天自动备份数据库
5. **日志审计**: 定期检查登录日志和操作日志
6. **更新依赖**: 定期更新系统依赖和安全补丁

## 性能优化建议

1. **数据库索引**: 为常用查询字段添加索引
2. **连接池**: 合理配置数据库连接池大小
3. **缓存**: 对频繁访问的数据添加缓存
4. **日志级别**: 生产环境使用info级别,避免debug
5. **静态资源**: 使用CDN加速前端资源

## 相关文档

- [部署指南](./DEPLOYMENT.md) - 详细的部署步骤
- [日志管理](./LOGGING.md) - 日志系统使用指南
- [故障排查](./TROUBLESHOOTING.md) - 常见问题解决方案
- [备份恢复](./BACKUP.md) - 数据备份和恢复流程

## 联系支持

如遇到无法解决的问题,请:
1. 收集相关日志
2. 记录问题复现步骤
3. 联系技术支持团队
