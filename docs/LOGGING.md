# 日志管理指南

## 日志系统概述

本系统使用 **Zap** 作为日志库,提供高性能的结构化日志功能。

### 日志特性

- ✅ **结构化日志**: JSON格式,便于机器解析
- ✅ **日志级别**: Debug, Info, Warn, Error, Fatal
- ✅ **自动轮转**: 自动按大小和时间轮转日志文件
- ✅ **请求追踪**: 自动记录所有HTTP请求详情
- ✅ **错误恢复**: 自动捕获panic并记录堆栈信息

## 日志级别说明

| 级别  | 用途 | 示例 |
|------|------|------|
| **Debug** | 调试信息,仅开发环境 | 查询参数、中间结果 |
| **Info** | 正常操作信息 | 用户登录、数据创建 |
| **Warn** | 警告信息,需要关注 | 登录失败、无效请求 |
| **Error** | 错误信息,需要处理 | 数据库错误、系统异常 |
| **Fatal** | 致命错误,程序退出 | 配置加载失败、数据库连接失败 |

## 日志配置

### 环境变量

```bash
# 日志文件路径
LOG_PATH=./logs/app.log

# 日志级别 (debug/info/warn/error)
LOG_LEVEL=info

# 运行环境 (development/production)
ENV=production
```

### Docker环境配置

在 `docker-compose.yml` 中:

```yaml
backend:
  environment:
    LOG_PATH: /app/logs/app.log
    LOG_LEVEL: info
    ENV: production
  volumes:
    - backend-logs:/app/logs
```

## 日志格式

### 结构化日志格式 (JSON)

生产环境使用JSON格式,便于日志分析工具处理:

```json
{
  "level": "info",
  "time": "2026-01-03T18:00:00+08:00",
  "caller": "handlers/auth.go:85",
  "msg": "User logged in successfully",
  "username": "admin",
  "user_id": 1,
  "ip": "192.168.1.100"
}
```

### 请求日志格式

每个HTTP请求都会自动记录:

```json
{
  "level": "info",
  "time": "2026-01-03T18:00:01+08:00",
  "caller": "middleware/logging.go:25",
  "msg": "Request completed",
  "status": 200,
  "method": "POST",
  "path": "/api/login",
  "query": "",
  "ip": "192.168.1.100",
  "user-agent": "Mozilla/5.0...",
  "latency": "15.234ms",
  "body_size": 256
}
```

### 错误日志格式

包含详细的错误信息和堆栈:

```json
{
  "level": "error",
  "time": "2026-01-03T18:00:02+08:00",
  "caller": "handlers/student.go:52",
  "msg": "Failed to create student",
  "error": "duplicate key value violates unique constraint",
  "stacktrace": "..."
}
```

## 日志查看方法

### Docker环境

```bash
# 实时查看日志
docker-compose logs -f backend

# 查看最近100行
docker-compose logs --tail=100 backend

# 查看特定时间段的日志
docker-compose logs --since="2026-01-03T10:00:00" backend

# 查看错误日志
docker-compose logs backend | grep '"level":"error"'

# 导出日志到文件
docker-compose logs backend > backend.log
```

### 本地部署

```bash
# 实时查看
tail -f logs/app.log

# 查看最近100行
tail -n 100 logs/app.log

# 查看错误日志
grep '"level":"error"' logs/app.log

# 查看特定用户的操作
grep '"username":"admin"' logs/app.log
```

## 日志分析示例

### 1. 查看登录活动

```bash
# 成功登录
grep "User logged in successfully" logs/app.log

# 失败登录
grep "Login failed" logs/app.log

# 统计登录失败次数
grep "Login failed" logs/app.log | wc -l

# 查看特定IP的登录尝试
grep '"ip":"192.168.1.100"' logs/app.log | grep "Login"
```

### 2. 性能分析

```bash
# 查看慢请求 (>2秒)
grep "Slow request detected" logs/app.log

# 查看所有请求的响应时间
grep "Request completed" logs/app.log | grep -o '"latency":"[^"]*"'

# 统计API调用次数
grep "Request completed" logs/app.log | grep -o '"path":"[^"]*"' | sort | uniq -c
```

### 3. 错误追踪

```bash
# 查看所有错误
grep '"level":"error"' logs/app.log

# 查看数据库错误
grep '"level":"error"' logs/app.log | grep -i "database"

# 查看panic错误
grep "Panic recovered" logs/app.log

# 按时间查看错误趋势
grep '"level":"error"' logs/app.log | grep -o '"time":"[^"]*"' | cut -d: -f1-2 | uniq -c
```

### 4. 用户行为分析

```bash
# 查看特定用户的所有操作
grep '"username":"admin"' logs/app.log

# 查看学生管理操作
grep -E "(Student created|Student updated|Student deleted)" logs/app.log

# 统计各类操作数量
grep -oE "(created|updated|deleted)" logs/app.log | sort | uniq -c
```

## 日志轮转配置

系统自动进行日志轮转,配置如下:

- **单文件最大大小**: 100MB
- **保留备份数**: 30个
- **保留天数**: 7天
- **压缩**: 是

配置位置: `tutor-backend/utils/logger.go`

```go
writer := &lumberjack.Logger{
    Filename:   logPath,
    MaxSize:    100, // MB
    MaxBackups: 30,  // 保留30个备份
    MaxAge:     7,   // 保留7天
    Compress:   true,
}
```

## 日志文件结构

```
logs/
├── app.log              # 当前日志文件
├── app-20260102.log.gz  # 昨天的日志 (已压缩)
├── app-20260101.log.gz  # 前天的日志
└── ...
```

## 使用日志分析工具

### 使用 jq 解析JSON日志

```bash
# 安装 jq
# Windows: choco install jq
# Linux: apt-get install jq
# Mac: brew install jq

# 美化输出
cat logs/app.log | jq '.'

# 只显示错误日志
cat logs/app.log | jq 'select(.level=="error")'

# 统计各级别日志数量
cat logs/app.log | jq -r '.level' | sort | uniq -c

# 查看最慢的10个请求
cat logs/app.log | jq 'select(.latency) | {path, latency}' | sort -k2 -r | head -10
```

### 使用 ELK Stack (可选)

对于大规模部署,建议使用 ELK Stack:

1. **Elasticsearch**: 存储和索引日志
2. **Logstash**: 收集和处理日志
3. **Kibana**: 可视化分析

配置示例:

```yaml
# docker-compose.yml
services:
  elasticsearch:
    image: elasticsearch:8.0.0
    
  logstash:
    image: logstash:8.0.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
      
  kibana:
    image: kibana:8.0.0
```

## 日志最佳实践

### 1. 生产环境配置

```bash
ENV=production
LOG_LEVEL=info  # 不要使用debug
LOG_PATH=/var/log/tutor/app.log
```

### 2. 开发环境配置

```bash
ENV=development
LOG_LEVEL=debug
LOG_PATH=./logs/app.log
```

### 3. 日志保留策略

- **开发环境**: 保留3天
- **测试环境**: 保留7天
- **生产环境**: 保留30天
- **重要日志**: 归档到对象存储

### 4. 敏感信息处理

⚠️ **注意**: 日志中不应包含:
- 用户密码
- JWT Token完整内容
- 信用卡信息
- 其他敏感个人信息

### 5. 日志监控告警

建议设置以下告警:

- 5分钟内错误日志 > 10条
- 慢请求 > 5个/分钟
- Panic错误 (立即告警)
- 登录失败 > 10次/分钟 (可能是暴力破解)

## 常见日志模式

### 正常启动日志

```
{"level":"info","msg":"Starting tutor management system","env":"production"}
{"level":"info","msg":"Database connected successfully"}
{"level":"info","msg":"Server starting on :8080"}
```

### 用户登录流程

```
{"level":"info","msg":"User logged in successfully","username":"admin"}
{"level":"info","msg":"Request completed","method":"POST","path":"/api/login","status":200}
```

### 数据操作流程

```
{"level":"info","msg":"Student created","student_id":123,"name":"张三"}
{"level":"info","msg":"Request completed","method":"POST","path":"/api/students","status":201}
```

### 错误场景

```
{"level":"warn","msg":"Login failed - user not found","username":"unknown"}
{"level":"error","msg":"Failed to create student","error":"database connection lost"}
{"level":"error","msg":"Panic recovered","error":"runtime error: invalid memory address"}
```

## 故障排查流程

1. **查看最近日志**: `tail -100 logs/app.log`
2. **搜索错误**: `grep error logs/app.log`
3. **分析请求**: 查看失败请求的完整上下文
4. **检查时间线**: 确定问题开始时间
5. **查看相关日志**: 追踪相关操作的完整流程

## 相关文档

- [运维指南](./OPERATIONS.md)
- [故障排查](./TROUBLESHOOTING.md)
- [部署指南](./DEPLOYMENT.md)
