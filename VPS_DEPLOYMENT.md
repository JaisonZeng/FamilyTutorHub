# Ubuntu VPS 部署指南 (2C2G 配置)

## 快速部署

### 方法一：使用自动部署脚本 (推荐)

```bash
# 1. 安装 git
sudo apt update
sudo apt install -y git

# 2. 克隆项目
git clone <your-repo-url> FamilyTutorHub
cd FamilyTutorHub

# 3. 添加执行权限并运行部署脚本
chmod +x deploy.sh
sudo ./deploy.sh
```

脚本会自动完成：
- 安装 Docker 和 Docker Compose
- 配置防火墙
- 检查端口占用
- 构建并启动服务
- 验证部署状态

---

### 方法二：手动部署

#### 1. 安装 Docker

```bash
# 更新包索引
sudo apt update

# 安装依赖
sudo apt install -y ca-certificates curl gnupg lsb-release

# 添加 Docker 官方 GPG key
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# 设置 Docker 仓库
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 验证安装
docker --version
docker compose version
```

#### 2. 克隆项目

```bash
# 推荐放在 /opt 目录（系统级第三方软件目录）
cd /opt
git clone <your-repo-url> FamilyTutorHub
cd FamilyTutorHub
```

#### 3. 配置防火墙

```bash
# 如果使用 ufw
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 22/tcp
sudo ufw enable

# 查看状态
sudo ufw status
```

#### 4. 构建并启动

```bash
# 构建镜像
docker compose build

# 启动服务
docker compose up -d

# 查看日志
docker compose logs -f
```

#### 5. 验证部署

```bash
# 检查容器状态
docker compose ps

# 检查后端健康
curl http://localhost:8080/health

# 检查数据库
docker compose exec db mysqladmin ping -h localhost
```

---

## 配置说明 (针对 2C2G VPS)

### 资源分配优化

当前 docker-compose.yml 已针对 2C2G 配置优化：

| 服务 | CPU 限制 | 内存限制 | 说明 |
|------|---------|---------|------|
| MySQL | 0.5 核 | 512MB | 数据库服务 |
| Backend | 0.5 核 | 512MB | Go 后端 |
| Frontend | 0.5 核 | 256MB | Nginx 前端 |

**总计**: 约 1.5 核 / 1.25GB 内存，预留约 75% 资源给系统和其他进程

### MySQL 优化配置

- `max_connections=100` - 最大连接数
- `innodb_buffer_pool_size=256M` - 缓冲池大小
- `innodb_log_file_size=64M` - 日志文件大小
- `table_open_cache=200` - 表缓存

### 端口配置

- **80** - 前端 (Nginx) - 对外开放
- **8080** - 后端 API - 仅本地访问 (127.0.0.1)
- **3306** - MySQL - 仅本地访问 (127.0.0.1)

---

## 访问信息

```
前端: http://你的VPS_IP
后端: http://localhost:8080 (仅内部访问)
API文档: http://你的VPS_IP/swagger
```

**默认账号**: admin / admin123

---

## 配置 HTTPS (可选但推荐)

### 使用 Certbot

```bash
# 安装 Certbot
sudo apt install -y certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run

# 查看证书
sudo certbot certificates
```

Certbot 会自动配置 nginx，无需手动修改配置。

---

## 备份设置

### 1. 设置备份脚本

```bash
# 添加执行权限
chmod +x scripts/backup.sh
```

### 2. 配置定时任务

```bash
# 编辑 crontab
crontab -e

# 添加以下行 (每天凌晨 2 点备份)
0 2 * * * /opt/FamilyTutorHub/scripts/backup.sh >> /var/log/tutor-backup.log 2>&1
```

### 3. 创建日志文件

```bash
sudo touch /var/log/tutor-backup.log
sudo chown $USER:$USER /var/log/tutor-backup.log
```

### 4. 手动测试备份

```bash
./scripts/backup.sh
```

---

## 常用命令

### 服务管理

```bash
# 启动服务
docker compose up -d

# 停止服务
docker compose stop

# 重启服务
docker compose restart

# 停止并删除容器
docker compose down

# 查看状态
docker compose ps

# 查看资源使用
docker stats
```

### 日志查看

```bash
# 查看所有日志
docker compose logs

# 查看后端日志
docker compose logs -f backend

# 查看数据库日志
docker compose logs -f db

# 查看最近 100 行
docker compose logs --tail=100 backend
```

### 数据库管理

```bash
# 进入 MySQL
docker compose exec db mysql -u tutor -ptutor123

# 备份数据库
docker compose exec db mysqldump -u tutor -ptutor123 tutor > backup.sql

# 恢复数据库
docker compose exec -T db mysql -u tutor -ptutor123 tutor < backup.sql
```

---

## 安全建议

### 1. 修改默认密码

```bash
# 修改数据库密码
docker compose exec db mysql -u root -proot123

# 在 MySQL 中执行
ALTER USER 'tutor'@'%' IDENTIFIED BY 'your_strong_password';
FLUSH PRIVILEGES;
EXIT;

# 更新 docker-compose.yml 中的密码
# 然后重启服务
docker compose down
docker compose up -d
```

### 2. 修改管理员密码

登录管理后台，在用户管理中修改 admin 密码

### 3. 配置 SSH 密钥登录

```bash
# 生成 SSH 密钥 (本地操作)
ssh-keygen -t rsa -b 4096

# 复制公钥到服务器
ssh-copy-id root@your-vps-ip

# 禁用密码登录
sudo nano /etc/ssh/sshd_config

# 修改以下行
PasswordAuthentication no

# 重启 SSH
sudo systemctl restart sshd
```

### 4. 安装 fail2ban

```bash
sudo apt install -y fail2ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
```

---

## 监控

### 查看资源使用

```bash
# 实时监控
htop

# Docker 资源使用
docker stats

# 磁盘使用
df -h

# 内存使用
free -h
```

### 健康检查脚本

```bash
#!/bin/bash
# /usr/local/bin/health-check.sh

HEALTH_URL="http://localhost:8080/health"

if ! curl -f $HEALTH_URL > /dev/null 2>&1; then
    echo "[$(date)] Health check failed!" >> /var/log/tutor-health.log
    # 可以添加发送邮件或通知的逻辑
fi
```

添加到 crontab 每 5 分钟检查一次：

```bash
*/5 * * * * /usr/local/bin/health-check.sh
```

---

## 故障排查

### 服务无法启动

```bash
# 查看详细日志
docker compose logs backend

# 检查端口占用
sudo lsof -i :80
sudo lsof -i :8080

# 检查磁盘空间
df -h
```

### 内存不足

```bash
# 查看内存使用
free -h

# 查看 Docker 内存使用
docker stats --no-stream

# 如果内存不足，可以调整 docker-compose.yml 中的资源限制
```

### 构建失败

```bash
# 清理 Docker 缓存
docker system prune -a

# 重新构建
docker compose build --no-cache
```

---

## 性能优化建议

### 1. 启用交换空间 (如果内存不足)

```bash
# 创建 2GB 交换文件
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 永久启用
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 优化交换性能
sudo sysctl vm.swappiness=10
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
```

### 2. 优化 Docker

```bash
# 编辑 Docker 配置
sudo nano /etc/docker/daemon.json

# 添加以下内容
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "5m",
    "max-file": "2"
  }
}

# 重启 Docker
sudo systemctl restart docker
```

---

## 更新部署

```bash
# 1. 备份数据
./scripts/backup.sh

# 2. 拉取最新代码
git pull origin main

# 3. 重新构建
docker compose build

# 4. 重启服务
docker compose down
docker compose up -d

# 5. 验证
docker compose ps
curl http://localhost/health
```

---

## 支持

如遇问题，请提供以下信息：

1. Ubuntu 版本: `lsb_release -a`
2. Docker 版本: `docker --version`
3. 容器状态: `docker compose ps`
4. 错误日志: `docker compose logs --tail=100`
