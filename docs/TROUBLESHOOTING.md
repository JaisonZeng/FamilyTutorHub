# 故障排查手册

## 快速诊断流程

```
问题发生
    ↓
检查健康状态 (/health)
    ↓
查看最近日志
    ↓
识别错误类型
    ↓
应用解决方案
    ↓
验证修复
```

## 常见问题及解决方案

### 1. 服务无法启动

#### 症状
- Docker容器启动后立即退出
- 访问 http://localhost:8080 无响应

#### 诊断步骤

```bash
# 1. 查看容器状态
docker-compose ps

# 2. 查看启动日志
docker-compose logs backend

# 3. 查看详细错误
docker-compose logs --tail=50 backend
```

#### 常见原因及解决方案

**原因1: 端口被占用**

```bash
# 检查端口占用
netstat -ano | findstr :8080

# 解决方案1: 停止占用进程
taskkill /PID <进程ID> /F

# 解决方案2: 修改端口
# 编辑 docker-compose.yml
ports:
  - "8081:8080"  # 改用8081端口
```

**原因2: 数据库连接失败**

日志特征:
```
{"level":"fatal","msg":"数据库连接失败","error":"..."}
```

解决方案:
```bash
# 检查数据库容器状态
docker-compose ps db

# 重启数据库
docker-compose restart db

# 检查数据库连接
docker-compose exec db mysql -u tutor -ptutor123 -e "SELECT 1"
```

**原因3: 配置文件错误**

日志特征:
```
{"level":"fatal","msg":"加载配置失败","error":"..."}
```

解决方案:
```bash
# 检查环境变量
docker-compose config

# 验证配置文件语法
cat config/config.yaml
```

---

### 2. 数据库连接问题

#### 症状
- API返回500错误
- 日志显示数据库错误
- 健康检查失败

#### 诊断步骤

```bash
# 1. 检查数据库容器
docker-compose ps db

# 2. 测试数据库连接
docker-compose exec db mysql -u tutor -ptutor123 tutor -e "SELECT 1"

# 3. 查看数据库日志
docker-compose logs db

# 4. 检查连接数
curl http://localhost:8080/metrics
```

#### 解决方案

**问题1: 数据库未就绪**

```bash
# 等待数据库完全启动
docker-compose up -d db
sleep 10
docker-compose up -d backend
```

**问题2: 连接数耗尽**

日志特征:
```
{"level":"error","msg":"too many connections"}
```

解决方案:
```bash
# 重启后端释放连接
docker-compose restart backend

# 或修改数据库配置
# 在 docker-compose.yml 中添加:
db:
  command: --max_connections=200
```

**问题3: 密码错误**

```bash
# 检查环境变量
docker-compose exec backend env | grep DB_

# 重置数据库密码
docker-compose exec db mysql -u root -proot123 -e "ALTER USER 'tutor'@'%' IDENTIFIED BY 'tutor123'"
```

---

### 3. API请求失败

#### 症状
- 前端显示网络错误
- API返回4xx或5xx错误
- 请求超时

#### 诊断步骤

```bash
# 1. 测试健康检查
curl http://localhost:8080/health

# 2. 测试具体API
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 3. 查看请求日志
docker-compose logs backend | grep "Request completed"

# 4. 查看错误日志
docker-compose logs backend | grep '"level":"error"'
```

#### 常见错误及解决方案

**401 Unauthorized - 认证失败**

```bash
# 检查token是否有效
# 日志中查看:
grep "Invalid JWT token" logs/app.log

# 解决方案: 重新登录获取新token
```

**404 Not Found - 路由不存在**

```bash
# 检查API路径是否正确
# 查看Swagger文档
http://localhost:8080/swagger/index.html
```

**500 Internal Server Error - 服务器错误**

```bash
# 查看详细错误
docker-compose logs backend | grep '"level":"error"' | tail -10

# 常见原因:
# - 数据库查询错误
# - 数据验证失败
# - 空指针异常
```

---

### 4. 性能问题

#### 症状
- 响应缓慢
- 请求超时
- CPU/内存占用高

#### 诊断步骤

```bash
# 1. 查看慢请求日志
docker-compose logs backend | grep "Slow request"

# 2. 检查资源使用
docker stats

# 3. 查看数据库连接
curl http://localhost:8080/metrics

# 4. 分析请求耗时
docker-compose logs backend | grep "latency" | tail -20
```

#### 解决方案

**问题1: 慢查询**

```bash
# 查看慢请求日志
grep "Slow request" logs/app.log

# 优化建议:
# - 添加数据库索引
# - 优化查询语句
# - 添加缓存
```

**问题2: 内存泄漏**

```bash
# 监控内存使用
docker stats --no-stream backend

# 临时解决: 重启服务
docker-compose restart backend

# 长期解决: 分析代码,修复内存泄漏
```

**问题3: 数据库连接池不足**

```bash
# 查看连接数
curl http://localhost:8080/metrics

# 如果 open_connections 接近上限,增加连接池:
# 修改 config/database.go
```

---

### 5. 日志问题

#### 症状
- 日志文件过大
- 磁盘空间不足
- 日志无法写入

#### 诊断步骤

```bash
# 1. 检查日志文件大小
docker-compose exec backend ls -lh /app/logs/

# 2. 检查磁盘空间
docker-compose exec backend df -h

# 3. 检查日志权限
docker-compose exec backend ls -la /app/logs/
```

#### 解决方案

**问题1: 日志文件过大**

```bash
# 手动清理旧日志
docker-compose exec backend find /app/logs/ -name "*.gz" -mtime +7 -delete

# 调整日志轮转配置
# 修改 utils/logger.go 中的 MaxSize, MaxBackups, MaxAge
```

**问题2: 磁盘空间不足**

```bash
# 清理Docker缓存
docker system prune -a

# 清理旧日志
docker-compose exec backend rm -f /app/logs/*.gz

# 增加磁盘空间或挂载新卷
```

**问题3: 日志级别过低**

```bash
# 生产环境应使用 info 级别
# 修改 docker-compose.yml:
environment:
  LOG_LEVEL: info  # 不要使用 debug
```

---

### 6. Docker相关问题

#### 症状
- 容器无法启动
- 网络连接问题
- 卷挂载失败

#### 诊断步骤

```bash
# 1. 检查Docker状态
docker info

# 2. 检查容器日志
docker-compose logs

# 3. 检查网络
docker network ls
docker network inspect familytutorhub_default

# 4. 检查卷
docker volume ls
docker volume inspect familytutorhub_backend-logs
```

#### 解决方案

**问题1: 网络问题**

```bash
# 重建网络
docker-compose down
docker network prune
docker-compose up -d
```

**问题2: 卷权限问题**

```bash
# 检查卷权限
docker-compose exec backend ls -la /app/logs

# 修复权限
docker-compose exec backend chown -R 1000:1000 /app/logs
```

**问题3: 镜像构建失败**

```bash
# 清理缓存重新构建
docker-compose build --no-cache backend

# 查看构建日志
docker-compose build backend
```

---

## 日志分析技巧

### 1. 追踪特定请求

```bash
# 通过IP追踪
grep '"ip":"192.168.1.100"' logs/app.log

# 通过用户追踪
grep '"username":"admin"' logs/app.log

# 通过时间范围追踪
grep '"time":"2026-01-03T10:' logs/app.log
```

### 2. 统计分析

```bash
# 统计错误数量
grep '"level":"error"' logs/app.log | wc -l

# 统计各API调用次数
grep "Request completed" logs/app.log | grep -o '"path":"[^"]*"' | sort | uniq -c

# 统计登录失败次数
grep "Login failed" logs/app.log | wc -l
```

### 3. 性能分析

```bash
# 找出最慢的请求
grep "latency" logs/app.log | sort -t: -k8 -r | head -10

# 统计平均响应时间
grep "latency" logs/app.log | grep -o '"latency":"[0-9.]*ms"' | awk -F'"' '{sum+=$4; count++} END {print sum/count "ms"}'
```

---

## 紧急情况处理

### 服务完全不可用

```bash
# 1. 立即重启所有服务
docker-compose restart

# 2. 如果仍然失败,完全重建
docker-compose down
docker-compose up -d

# 3. 查看错误日志
docker-compose logs -f

# 4. 如果数据库损坏,从备份恢复
docker-compose exec -T db mysql -u tutor -ptutor123 tutor < backup_latest.sql
```

### 数据丢失

```bash
# 1. 立即停止服务
docker-compose stop

# 2. 从最近备份恢复
docker-compose up -d db
docker-compose exec -T db mysql -u tutor -ptutor123 tutor < backup_YYYYMMDD.sql

# 3. 验证数据
docker-compose exec db mysql -u tutor -ptutor123 tutor -e "SELECT COUNT(*) FROM students"

# 4. 重启服务
docker-compose up -d
```

### 安全事件

```bash
# 1. 查看异常登录
grep "Login failed" logs/app.log | tail -100

# 2. 查看可疑IP
grep '"level":"warn"' logs/app.log | grep -o '"ip":"[^"]*"' | sort | uniq -c | sort -rn

# 3. 临时封禁IP (需要配置防火墙)
# 或修改代码添加IP黑名单

# 4. 修改密码
docker-compose exec db mysql -u root -proot123 -e "ALTER USER 'tutor'@'%' IDENTIFIED BY 'new_password'"
```

---

## 预防措施

### 1. 监控告警

设置以下监控:
- 健康检查失败 → 立即告警
- 错误率 > 5% → 告警
- 慢请求 > 10/分钟 → 告警
- 磁盘使用 > 80% → 告警

### 2. 定期维护

```bash
# 每天: 检查日志
grep '"level":"error"' logs/app.log | tail -20

# 每周: 清理旧日志
find logs/ -name "*.gz" -mtime +30 -delete

# 每月: 数据库优化
docker-compose exec db mysqlcheck -u tutor -ptutor123 --optimize tutor
```

### 3. 备份策略

```bash
# 每天自动备份 (添加到crontab)
0 2 * * * cd /path/to/project && docker-compose exec -T db mysqldump -u tutor -ptutor123 tutor > backup_$(date +\%Y\%m\%d).sql
```

---

## 联系支持

如果以上方法都无法解决问题:

1. **收集信息**:
   - 完整的错误日志
   - 问题复现步骤
   - 系统环境信息

2. **创建问题报告**:
   ```bash
   # 导出日志
   docker-compose logs > debug_logs.txt
   
   # 导出配置
   docker-compose config > debug_config.yml
   
   # 系统信息
   docker info > debug_system.txt
   ```

3. **联系技术支持**,提供以上文件

---

## 相关文档

- [运维指南](./OPERATIONS.md)
- [日志管理](./LOGGING.md)
- [部署指南](./DEPLOYMENT.md)
- [备份恢复](./BACKUP.md)
