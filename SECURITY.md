# 安全防护指南

## 概述

本项目已实现多层安全防护机制，防止恶意流量和攻击。

---

## 已实现的安全机制

### 1. Nginx 层防护 (前端)

#### 速率限制
- **一般请求**: 30 请求/秒，突发 50
- **API 请求**: 10 请求/秒，突发 20
- **Swagger 文档**: 30 请求/秒，突发 10
- **静态资源**: 30 请求/秒，突发 100

#### 连接限制
- 单个 IP 最多 10 个并发连接
- API 路由最多 5 个并发连接

#### 安全配置
- 隐藏 Nginx 版本号
- 限制 HTTP 方法 (仅允许 GET, POST, PUT, DELETE, OPTIONS)
- 阻止访问隐藏文件 (`.` 开头)
- 超时保护 (60 秒)

**配置文件**: [tutor-admin/nginx.conf](tutor-admin/nginx.conf)

---

### 2. 后端层防护 (Go)

#### IP 黑名单
- 动态 IP 黑名单管理
- 支持单个 IP 和 CIDR 格式 (如 192.168.1.0/24)
- 自动检查和拦截

**文件**: [tutor-backend/middleware/blacklist.go](tutor-backend/middleware/blacklist.go)

#### 速率限制
- **全局限流**: 30 请求/秒，突发 50
- **API 限流**: 10 请求/秒，突发 15
- 自动清理过期限流器 (每 5 分钟)

**文件**: [tutor-backend/middleware/ratelimit.go](tutor-backend/middleware/ratelimit.go)

#### 安全响应头
- `X-Frame-Options: DENY` - 防止点击劫持
- `X-Content-Type-Options: nosniff` - 防止 MIME 嗅探
- `X-XSS-Protection: 1; mode=block` - XSS 保护
- `Referrer-Policy: strict-origin-when-cross-origin` - 限制引用来源
- `Content-Security-Policy` - 内容安全策略
- 隐藏服务器信息

**文件**: [tutor-backend/middleware/security.go](tutor-backend/middleware/security.go)

#### 日志监控
- 结构化日志记录所有请求
- 慢请求检测 (>2 秒)
- 错误日志追踪

---

## 部署安全配置

### 1. 修改默认密码

```bash
# 修改数据库密码
docker compose exec db mysql -u root -proot123
ALTER USER 'tutor'@'%' IDENTIFIED BY 'your_strong_password';
FLUSH PRIVILEGES;

# 更新 docker-compose.yml
# 然后重启服务
docker compose down
docker compose up -d
```

### 2. 配置防火墙

```bash
# 安装 ufw
sudo apt install -y ufw

# 默认拒绝所有传入
sudo ufw default deny incoming

# 允许 SSH
sudo ufw allow 22/tcp

# 允许 HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 启用防火墙
sudo ufw enable

# 查看状态
sudo ufw status verbose
```

### 3. 限制 Docker 端口

当前配置已优化：
- `80` - 前端 (对外开放)
- `8080` - 后端 (仅本地: 127.0.0.1)
- `3306` - 数据库 (仅本地: 127.0.0.1)

**重要**: 生产环境中，后端和数据库不应直接对外暴露。

### 4. 配置 HTTPS

```bash
# 安装 Certbot
sudo apt install -y certbot python3-certbot-nginx

# 获取免费 SSL 证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run

# 添加定时任务
sudo crontab -e
# 添加: 0 0 * * * certbot renew --quiet
```

---

## 安全监控工具

### 1. 使用安全管理脚本

```bash
# 添加执行权限
chmod +x scripts/security-tools.sh

# 运行工具
./scripts/security-tools.sh
```

功能：
- 查看 IP 黑名单
- 添加/移除黑名单
- 查看错误日志
- 查看被拒绝的请求
- 查看访问量最高的 IP
- 查看系统资源使用
- 一键封禁可疑 IP

### 2. 手动监控命令

```bash
# 查看最近的 429 错误 (限流)
docker compose logs backend | grep "429"

# 查看最近的 403 错误 (黑名单)
docker compose logs backend | grep "403"

# 查看慢请求
docker compose logs backend | grep "slow request"

# 实时监控日志
docker compose logs -f backend

# 查看访问最多的 IP
docker compose logs backend | grep -oE '([0-9]{1,3}\.){3}[0-9]{1,3}' | sort | uniq -c | sort -rn | head -20
```

---

## 应急响应

### 发现攻击时的处理步骤

#### 1. 立即封禁攻击 IP

```bash
# 方法一：使用安全工具
./scripts/security-tools.sh
# 选择选项 2 添加 IP 到黑名单

# 方法二：修改代码
# 在 middleware/blacklist.go 的 InitBlacklist() 函数中添加：
blacklist["攻击IP"] = true

# 重启服务
docker compose restart backend
```

#### 2. 临时降低限流阈值

编辑 [tutor-backend/middleware/ratelimit.go](tutor-backend/middleware/ratelimit.go):

```go
// 临时降低到 5 请求/秒
apiLimiter = NewIPRateLimiter(5, 10)
```

#### 3. 启用维护模式

如果攻击严重，可以临时关闭服务：

```bash
# 停止服务
docker compose stop

# 或只停止前端
docker compose stop frontend

# 或只停止后端
docker compose stop backend
```

#### 4. 收集证据

```bash
# 导出日志
docker compose logs backend > attack_$(date +%Y%m%d_%H%M%S).log

# 分析攻击模式
grep -i "attack\|injection\|xss" attack_*.log
```

---

## DDoS 防护建议

### 1. 使用云服务提供商的 DDoS 防护

- **Cloudflare**: 免费套餐提供基础 DDoS 防护
- **AWS Shield**: AWS 用户可用
- **阿里云 DDoS 防护**: 国内用户推荐

### 2. 配置 Cloudflare (推荐)

1. 注册 Cloudflare 账号
2. 添加你的域名
3. 修改域名 NS 服务器到 Cloudflare
4. 启用以下功能：
   - **Under Attack Mode**: 攻击时启用
   - **Rate Limiting**: 付费功能
   - **Browser Integrity Check**: 免费功能
   - **Hotlink Protection**: 防止盗链

### 3. 限流配置建议

根据用户规模调整限流参数：

| 用户规模 | 每秒请求数 | 突发请求数 |
|---------|----------|-----------|
| 小型 (<100) | 10 | 20 |
| 中型 (100-1000) | 30 | 50 |
| 大型 (>1000) | 50 | 100 |

---

## 常见攻击类型及防护

### 1. SQL 注入
- **防护**: 使用参数化查询 (GORM 自动处理)
- **检测**: 监控日志中的 SQL 关键字

### 2. XSS 攻击
- **防护**: CSP 头、输入验证、输出转义
- **检测**: 监控异常的脚本标签

### 3. CSRF 攻击
- **防护**: JWT Token、SameSite Cookie
- **检测**: 验证 Referer 头

### 4. 暴力破解
- **防护**: 速率限制、IP 黑名单、账户锁定
- **检测**: 监控多次失败登录

### 5. DDoS 攻击
- **防护**: 多层限流、云防护
- **检测**: 流量异常激增

---

## 安全检查清单

部署后必须完成的安全配置：

- [ ] 修改所有默认密码
- [ ] 配置防火墙规则
- [ ] 配置 HTTPS 证书
- [ ] 限制容器端口 (只开放 80/443)
- [ ] 配置 CORS 白名单
- [ ] 设置日志监控
- [ ] 配置自动备份
- [ ] 测试限流功能
- [ ] 配置云服务 DDoS 防护 (可选)
- [ ] 设置安全告警

---

## 最佳实践

### 1. 定期更新

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 更新 Docker 镜像
docker compose pull
docker compose up -d --build
```

### 2. 定期备份

```bash
# 设置每日备份
crontab -e
# 添加: 0 2 * * * /root/FamilyTutorHub/scripts/backup.sh
```

### 3. 日志轮转

Docker 日志已在 docker-compose.yml 中配置自动轮转 (最大 5MB，保留 2 个文件)

### 4. 监控告警

建议使用监控服务：
- **免费**: UptimeRobot, Pingdom
- **付费**: Datadog, New Relic

---

## 安全资源

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)
- [CWE - Common Weakness Enumeration](https://cwe.mitre.org/)

---

## 紧急联系

如发现安全漏洞或遭受攻击：
1. 立即封禁攻击 IP
2. 收集日志和证据
3. 评估损失和影响范围
4. 必要时联系安全专家
