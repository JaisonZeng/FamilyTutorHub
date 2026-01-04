# 部署指南

## 环境要求

### 硬件要求

**最低配置**:
- CPU: 2核
- 内存: 4GB
- 磁盘: 20GB

**推荐配置**:
- CPU: 4核
- 内存: 8GB
- 磁盘: 50GB SSD

### 软件要求

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **操作系统**: Linux / Windows / macOS

## 部署方式

### 方式一: Docker Compose 部署 (推荐)

#### 1. 准备工作

```bash
# 克隆项目
git clone <repository-url>
cd FamilyTutorHub

# 检查Docker版本
docker --version
docker-compose --version
```

#### 2. 配置环境变量

创建 `.env` 文件 (可选):

```bash
# 数据库配置
DB_HOST=db
DB_PORT=3306
DB_USER=tutor
DB_PASSWORD=tutor123  # 生产环境请修改
DB_NAME=tutor

# 后端配置
ENV=production
LOG_PATH=/app/logs/app.log
LOG_LEVEL=info

# 端口配置
BACKEND_PORT=8080
FRONTEND_PORT=3000
```

#### 3. 启动服务

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看启动日志
docker-compose logs -f

# 等待服务就绪 (约30秒)
```

#### 4. 验证部署

```bash
# 检查服务状态
docker-compose ps

# 测试健康检查
curl http://localhost:8080/health

# 测试就绪检查
curl http://localhost:8080/health/ready

# 访问管理后台
# 浏览器打开: http://localhost:3000
# 默认账号: admin / admin123
```

#### 5. 查看日志

```bash
# 查看所有服务日志
docker-compose logs

# 查看后端日志
docker-compose logs -f backend

# 查看数据库日志
docker-compose logs -f db
```

---

### 方式二: 手动部署

#### 1. 部署数据库

```bash
# 安装MySQL 8.0
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install mysql-server

# 创建数据库和用户
mysql -u root -p
```

```sql
CREATE DATABASE tutor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'tutor'@'localhost' IDENTIFIED BY 'tutor123';
GRANT ALL PRIVILEGES ON tutor.* TO 'tutor'@'localhost';
FLUSH PRIVILEGES;
```

#### 2. 部署后端

```bash
cd tutor-backend

# 安装Go 1.21+
# 下载: https://golang.org/dl/

# 安装依赖
go mod download

# 配置环境变量
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=tutor
export DB_PASSWORD=tutor123
export DB_NAME=tutor
export ENV=production
export LOG_PATH=./logs/app.log
export LOG_LEVEL=info

# 编译
go build -o tutor-backend main.go

# 运行
./tutor-backend
```

#### 3. 部署前端

```bash
cd tutor-admin

# 安装Node.js 16+
# 下载: https://nodejs.org/

# 安装依赖
npm install

# 构建生产版本
npm run build

# 使用nginx部署
# 将 dist/ 目录内容复制到 nginx 网站根目录
sudo cp -r dist/* /var/www/html/
```

#### 4. 配置Nginx

```nginx
# /etc/nginx/sites-available/tutor
server {
    listen 80;
    server_name your-domain.com;

    # 前端
    location / {
        root /var/www/html;
        try_files $uri $uri/ /index.html;
    }

    # 后端API
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 健康检查
    location /health {
        proxy_pass http://localhost:8080;
    }
}
```

```bash
# 启用配置
sudo ln -s /etc/nginx/sites-available/tutor /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## 生产环境配置

### 1. 安全配置

#### 修改默认密码

```bash
# 修改数据库密码
docker-compose exec db mysql -u root -proot123

# 在MySQL中执行:
ALTER USER 'tutor'@'%' IDENTIFIED BY 'your_strong_password';
FLUSH PRIVILEGES;

# 更新 docker-compose.yml 中的密码
```

#### 修改管理员密码

```bash
# 登录后台后,在用户管理中修改密码
# 或通过API修改
```

#### 配置HTTPS

```bash
# 使用 Let's Encrypt 获取免费证书
sudo apt-get install certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

### 2. 性能优化

#### 数据库优化

```sql
-- 添加索引
ALTER TABLE students ADD INDEX idx_name (name);
ALTER TABLE schedules ADD INDEX idx_date (date);
ALTER TABLE exam_results ADD INDEX idx_student (student_id);

-- 优化配置
-- 编辑 /etc/mysql/my.cnf
[mysqld]
max_connections = 200
innodb_buffer_pool_size = 1G
query_cache_size = 64M
```

#### 后端优化

```yaml
# docker-compose.yml
backend:
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 2G
      reservations:
        cpus: '1'
        memory: 1G
```

### 3. 日志配置

```yaml
# docker-compose.yml
backend:
  environment:
    LOG_LEVEL: info  # 生产环境使用info
    LOG_PATH: /app/logs/app.log
  volumes:
    - backend-logs:/app/logs
  logging:
    driver: "json-file"
    options:
      max-size: "10m"
      max-file: "3"
```

### 4. 备份配置

创建自动备份脚本:

```bash
# /usr/local/bin/backup-tutor.sh
#!/bin/bash
BACKUP_DIR=/backup/tutor
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份数据库
docker-compose exec -T db mysqldump -u tutor -ptutor123 tutor > $BACKUP_DIR/db_$DATE.sql

# 压缩
gzip $BACKUP_DIR/db_$DATE.sql

# 删除30天前的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete

echo "Backup completed: $BACKUP_DIR/db_$DATE.sql.gz"
```

```bash
# 添加到crontab
chmod +x /usr/local/bin/backup-tutor.sh
crontab -e

# 每天凌晨2点备份
0 2 * * * /usr/local/bin/backup-tutor.sh >> /var/log/tutor-backup.log 2>&1
```

---

## 监控配置

### 1. 健康检查监控

```bash
# 创建监控脚本
# /usr/local/bin/health-check.sh
#!/bin/bash

HEALTH_URL="http://localhost:8080/health"
ALERT_EMAIL="admin@example.com"

if ! curl -f $HEALTH_URL > /dev/null 2>&1; then
    echo "Health check failed!" | mail -s "Tutor System Alert" $ALERT_EMAIL
fi
```

```bash
# 每5分钟检查一次
*/5 * * * * /usr/local/bin/health-check.sh
```

### 2. 日志监控

```bash
# 监控错误日志
# /usr/local/bin/log-monitor.sh
#!/bin/bash

ERROR_COUNT=$(docker-compose logs --since 5m backend | grep '"level":"error"' | wc -l)

if [ $ERROR_COUNT -gt 10 ]; then
    echo "Error count in last 5 minutes: $ERROR_COUNT" | mail -s "High Error Rate Alert" admin@example.com
fi
```

---

## 升级部署

### 1. 备份数据

```bash
# 备份数据库
docker-compose exec -T db mysqldump -u tutor -ptutor123 tutor > backup_before_upgrade.sql

# 备份日志
docker-compose exec backend tar czf /tmp/logs.tar.gz /app/logs
docker cp $(docker-compose ps -q backend):/tmp/logs.tar.gz ./logs_backup.tar.gz
```

### 2. 拉取新代码

```bash
git pull origin main
```

### 3. 更新服务

```bash
# 重新构建镜像
docker-compose build

# 停止旧服务
docker-compose down

# 启动新服务
docker-compose up -d

# 查看启动日志
docker-compose logs -f
```

### 4. 验证升级

```bash
# 检查服务状态
docker-compose ps

# 测试健康检查
curl http://localhost:8080/health

# 检查日志
docker-compose logs backend | grep -i error
```

### 5. 回滚 (如果需要)

```bash
# 停止服务
docker-compose down

# 回滚代码
git reset --hard <previous-commit>

# 恢复数据库
docker-compose up -d db
docker-compose exec -T db mysql -u tutor -ptutor123 tutor < backup_before_upgrade.sql

# 重新启动
docker-compose up -d
```

---

## 首次部署检查清单

- [ ] 环境要求已满足
- [ ] Docker和Docker Compose已安装
- [ ] 代码已克隆
- [ ] 环境变量已配置
- [ ] 默认密码已修改
- [ ] 服务已启动
- [ ] 健康检查通过
- [ ] 前端可访问
- [ ] 可以登录管理后台
- [ ] 数据库连接正常
- [ ] 日志正常输出
- [ ] 备份脚本已配置
- [ ] 监控已设置
- [ ] HTTPS已配置 (生产环境)
- [ ] 防火墙规则已配置

---

## 常见部署问题

详见 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)

### 快速问题排查

```bash
# 服务无法启动
docker-compose logs backend

# 端口冲突
netstat -ano | findstr :8080

# 数据库连接失败
docker-compose exec db mysql -u tutor -ptutor123 -e "SELECT 1"

# 权限问题
docker-compose exec backend ls -la /app/logs
```

---

## 相关文档

- [运维指南](./OPERATIONS.md)
- [日志管理](./LOGGING.md)
- [故障排查](./TROUBLESHOOTING.md)
- [备份恢复](./BACKUP.md)

---

## 技术支持

如遇部署问题,请提供:
1. 操作系统版本
2. Docker版本
3. 错误日志
4. 部署步骤

联系方式: [待补充]
