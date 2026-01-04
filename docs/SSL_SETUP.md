# SSL/HTTPS 配置完整指南

## 📋 目录

1. [快速开始](#快速开始)
2. [自动配置（推荐）](#自动配置推荐)
3. [手动配置](#手动配置)
4. [Cloudflare 配置](#cloudflare-配置)
5. [证书续期](#证书续期)
6. [故障排查](#故障排查)

---

## 快速开始

### 方法 1: 使用自动配置脚本（推荐）

```bash
# 在 VPS 上执行
cd /opt/FamilyTutorHub
chmod +x scripts/setup-ssl.sh
sudo ./scripts/setup-ssl.sh
```

脚本会自动完成：
- ✅ 安装 Certbot
- ✅ 获取免费 SSL 证书
- ✅ 配置 Nginx HTTPS
- ✅ 设置自动续期
- ✅ 重启服务

### 方法 2: Cloudflare Flexible SSL（最简单）

**不需要配置 VPS，只需在 Cloudflare 设置：**

1. 登录 [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. 选择您的域名
3. 进入 **SSL/TLS** → **Overview**
4. 选择 **"Flexible"** 模式
5. 等待几分钟，访问 `https://familytutorhub.jai-squidward.top`

**优点**：
- ✅ 无需配置 VPS
- ✅ 立即生效
- ✅ 免费

**缺点**：
- ❌ Cloudflare 到 VPS 不是加密的
- ❌ 依赖 Cloudflare 服务

---

## 自动配置（推荐）

### 前置要求

- 域名已指向 VPS IP
- 80 和 443 端口未占用
- VPS 有 root 权限

### 执行步骤

```bash
# 1. SSH 连接到 VPS
ssh root@your-vps-ip

# 2. 进入项目目录
cd /opt/FamilyTutorHub

# 3. 运行配置脚本
chmod +x scripts/setup-ssl.sh
sudo ./scripts/setup-ssl.sh

# 4. 按提示输入信息
# 域名: familytutorhub.jai-squidward.top
# 邮箱: your-email@example.com
```

### 配置后验证

```bash
# 检查证书
certbot certificates

# 测试 HTTPS
curl -I https://familytutorhub.jai-squidward.top

# 应该返回:
# HTTP/1.1 200 OK
# Server: nginx
```

---

## 手动配置

如果自动脚本失败，可以手动配置：

### 1. 安装 Certbot

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install certbot

# CentOS/RHEL
sudo yum install certbot
```

### 2. 停止前端容器

```bash
cd /opt/FamilyTutorHub
docker compose stop frontend
```

### 3. 获取证书

```bash
sudo certbot certonly --standalone -d familytutorhub.jai-squidward.top
```

### 4. 复制证书

```bash
sudo mkdir -p /opt/FamilyTutorHub/ssl
sudo cp /etc/letsencrypt/live/familytutorhub.jai-squidward.top/fullchain.pem /opt/FamilyTutorHub/ssl/
sudo cp /etc/letsencrypt/live/familytutorhub.jai-squidward.top/privkey.pem /opt/FamilyTutorHub/ssl/
sudo chmod 644 /opt/FamilyTutorHub/ssl/*.pem
```

### 5. 使用 SSL 配置文件

```bash
# 使用 SSL 版本的 docker-compose
docker compose -f docker-compose.ssl.yml up -d
```

---

## Cloudflare 配置

### SSL/TLS 模式选择

根据您的配置选择合适的模式：

| 模式 | Cloudflare → 用户 | Cloudflare → VPS | 适合场景 |
|------|-------------------|------------------|----------|
| **Off** | ❌ | ❌ | 不推荐 |
| **Flexible** | ✅ | ❌ | VPS 无证书时 |
| **Full** | ✅ | ✅ | VPS 有自签名证书 |
| **Full (strict)** | ✅ | ✅ | **推荐** - VPS 有有效证书 |

### 推荐配置

**如果您使用了 Let's Encrypt 证书：**

1. Cloudflare SSL/TLS → **"Full (strict)"**
2. Edge Certificates → 开启 "Always Use HTTPS"
3. Edge Certificates → 开启 "Automatic HTTPS Rewrites"

**如果使用 Cloudflare Flexible：**

1. Cloudflare SSL/TLS → **"Flexible"**
2. 无需配置 VPS

### DNS 配置

确保 DNS 记录正确：

```
Type: A
Name: familytutorhub
IPv4 address: YOUR_VPS_IP
Proxy: ✅ (橙色云朵)
TTL: Auto
```

---

## 证书续期

Let's Encrypt 证书有效期为 90 天，需要定期续期。

### 自动续期（已配置）

```bash
# 查看续期任务
crontab -l | grep renew-ssl

# 手动测试续期
/opt/FamilyTutorHub/scripts/renew-ssl.sh
```

### 手动续期

```bash
# 续期证书
sudo certbot renew

# 复制到项目目录
sudo cp /etc/letsencrypt/live/familytutorhub.jai-squidward.top/*.pem /opt/FamilyTutorHub/ssl/

# 重启前端
docker compose restart frontend
```

---

## 故障排查

### 问题 1: 证书获取失败

**错误**: `Could not bind to IPv4 or IPv6`

**解决**:
```bash
# 检查 80 端口占用
sudo lsof -i :80

# 停止占用端口的服务
docker compose stop frontend
```

### 问题 2: HTTPS 无法访问

**检查清单**:
```bash
# 1. 检查容器状态
docker compose ps

# 2. 检查证书文件
ls -la /opt/FamilyTutorHub/ssl/

# 3. 检查 443 端口
sudo netstat -tlnp | grep :443

# 4. 查看 nginx 日志
docker logs frontend
```

### 问题 3: Cloudflare 521 错误

**解决**:
1. 检查源服务器是否运行
2. 验证 DNS A 记录
3. 检查防火墙规则
4. 尝试 DNS-only 模式

### 问题 4: 混合内容警告

**原因**: HTTPS 页面包含 HTTP 资源

**解决**: 确保所有资源使用 HTTPS 或相对路径

---

## 安全最佳实践

### 1. 启用 HSTS

在 nginx 配置中添加：
```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
```

### 2. 使用强加密套件

```nginx
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256...';
ssl_prefer_server_ciphers on;
```

### 3. 定期更新证书

- 设置 cron 任务自动续期
- 监控证书过期时间
- 配置邮件提醒

### 4. 测试 SSL 配置

使用在线工具检测：
- https://www.ssllabs.com/ssltest/
- https://observatory.mozilla.org/

---

## 性能优化

### 启用 HTTP/2

```nginx
listen 443 ssl http2;
```

### 会话缓存

```nginx
ssl_session_cache shared:SSL:10m;
ssl_session_timeout 10m;
```

### OCSP Stapling

```nginx
ssl_stapling on;
ssl_stapling_verify on;
```

---

## 成本对比

| 方案 | 成本 | 安全性 | 难度 |
|------|------|--------|------|
| **Cloudflare Flexible** | 免费 | ⭐⭐ | 简单 |
| **Let's Encrypt** | 免费 | ⭐⭐⭐⭐ | 中等 |
| **付费证书** | $50-200/年 | ⭐⭐⭐⭐⭐ | 简单 |

---

## 快速决策树

```
需要配置 HTTPS?
├─ 想要最简单的方式？
│  └─ 使用 Cloudflare Flexible
│
├─ 想要最高安全性？
│  └─ 使用 Let's Encrypt + Cloudflare Full (strict)
│
└─ 有预算且要最高信任度？
   └─ 购买付费 SSL 证书
```

---

## 总结

**推荐方案**: Let's Encrypt + Cloudflare Full (strict)

- ✅ 完全免费
- ✅ 端到端加密
- ✅ 自动续期
- ✅ 高性能 CDN
- ✅ DDoS 防护

**下一步**: 运行自动配置脚本或使用 Cloudflare Flexible 快速启用 HTTPS。
