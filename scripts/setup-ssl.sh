#!/bin/bash

# SSL 证书自动配置脚本
# 使用 Let's Encrypt 免费证书

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}==================================="
echo "  FamilyTutorHub SSL 证书配置"
echo -e "===================================${NC}"
echo ""

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}请使用 sudo 运行此脚本${NC}"
    exit 1
fi

# 获取域名
read -p "请输入您的域名 (例如: familytutorhub.jai-squidward.top): " DOMAIN
read -p "请输入您的邮箱 (用于证书提醒): " EMAIL

echo ""
echo -e "${YELLOW}配置信息：${NC}"
echo "域名: $DOMAIN"
echo "邮箱: $EMAIL"
echo ""
read -p "确认以上信息正确? (y/n): " confirm
if [ "$confirm" != "y" ]; then
    echo "已取消"
    exit 1
fi

# 1. 安装 Certbot
echo ""
echo -e "${YELLOW}[1/5] 安装 Certbot...${NC}"
apt update
apt install -y certbot

# 2. 停止前端容器（释放 80 端口）
echo ""
echo -e "${YELLOW}[2/5] 停止前端容器...${NC}"
cd /opt/FamilyTutorHub
docker compose stop frontend

# 3. 获取 SSL 证书
echo ""
echo -e "${YELLOW}[3/5] 获取 SSL 证书...${NC}"
certbot certonly --standalone \
    -d $DOMAIN \
    --email $EMAIL \
    --agree-tos \
    --non-interactive \
    --force-renewal

if [ $? -ne 0 ]; then
    echo -e "${RED}证书获取失败${NC}"
    docker compose start frontend
    exit 1
fi

# 4. 创建 SSL 目录
echo ""
echo -e "${YELLOW}[4/5] 配置 SSL 证书...${NC}"
mkdir -p /opt/FamilyTutorHub/ssl

# 复制证书（可读权限）
cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem /opt/FamilyTutorHub/ssl/
cp /etc/letsencrypt/live/$DOMAIN/privkey.pem /opt/FamilyTutorHub/ssl/
chmod 644 /opt/FamilyTutorHub/ssl/*.pem

# 5. 更新 docker-compose.yml
echo ""
echo -e "${YELLOW}[5/5] 更新 Docker 配置...${NC}"

# 备份原配置
cp docker-compose.yml docker-compose.yml.backup

# 检查是否已存在 SSL 配置
if grep -q "443:443" docker-compose.yml; then
    echo -e "${GREEN}SSL 配置已存在，跳过...${NC}"
else
    # 在前端服务中添加 SSL 配置
    sed -i '/frontend:/,/# restart: unless-stopped/ {
        s/ports:$/ports:\n      - "80:80"\n      - "443:443"/
        s/- "80:80"$/- "443:443"/
    }' docker-compose.yml

    # 添加证书卷挂载
    sed -i '/frontend:/,/restart: unless-stopped/ {
        /volumes:/i\    volumes:\n      - \./ssl:/etc/nginx/ssl:ro
    }' docker-compose.yml

    echo -e "${GREEN}✓ Docker 配置已更新${NC}"
fi

# 6. 创建 nginx SSL 配置
cat > /opt/FamilyTutorHub/nginx-ssl.conf <<'EOF'
server {
    listen 443 ssl http2;
    server_name _;

    # SSL 证书配置
    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;

    # SSL 优化配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # 安全头
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;

    # 其他配置保持不变
    root /usr/share/nginx/html;
    index index.html;

    # 所有其他配置保持不变...
    include /etc/nginx/conf.d/default.conf;
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name _;
    return 301 https://$host$request_uri;
}
EOF

# 7. 设置证书自动续期
echo ""
echo -e "${YELLOW}配置证书自动续期...${NC}"

# 创建续期脚本
cat > /opt/FamilyTutorHub/scripts/renew-ssl.sh <<'EOF'
#!/bin/bash
# SSL 证书续期脚本

echo "$(date): 检查 SSL 证书..."

# 续期证书
certbot renew --quiet

# 复制新证书
if [ -f /etc/letsencrypt/live/YOUR_DOMAIN/fullchain.pem ]; then
    cp /etc/letsencrypt/live/YOUR_DOMAIN/fullchain.pem /opt/FamilyTutorHub/ssl/
    cp /etc/letsencrypt/live/YOUR_DOMAIN/privkey.pem /opt/FamilyTutorHub/ssl/
    chmod 644 /opt/FamilyTutorHub/ssl/*.pem

    # 重启前端容器
    cd /opt/FamilyTutorHub
    docker compose restart frontend

    echo "$(date): 证书已更新并重启服务"
fi
EOF

# 替换域名占位符
sed -i "s/YOUR_DOMAIN/$DOMAIN/g" /opt/FamilyTutorHub/scripts/renew-ssl.sh

chmod +x /opt/FamilyTutorHub/scripts/renew-ssl.sh

# 添加到 crontab（每月 1 号凌晨 3 点）
(crontab -l 2>/dev/null | grep -v "renew-ssl.sh"; echo "0 3 1 * * /opt/FamilyTutorHub/scripts/renew-ssl.sh >> /var/log/ssl-renew.log 2>&1") | crontab -

echo -e "${GREEN}✓ 证书自动续期已配置（每月 1 号检查）${NC}"

# 8. 重启服务
echo ""
echo -e "${YELLOW}重启服务...${NC}"
docker compose up -d

# 等待服务启动
sleep 5

# 9. 测试 HTTPS
echo ""
echo -e "${YELLOW}测试 HTTPS 连接...${NC}"
if curl -skf https://$DOMAIN > /dev/null; then
    echo -e "${GREEN}✓ HTTPS 配置成功！${NC}"
else
    echo -e "${RED}✗ HTTPS 测试失败，请检查配置${NC}"
fi

# 完成
echo ""
echo -e "${GREEN}===================================${NC}"
echo -e "${GREEN}  SSL 证书配置完成！${NC}"
echo -e "${GREEN}===================================${NC}"
echo ""
echo "访问地址："
echo -e "  HTTP:  http://$DOMAIN (会重定向到 HTTPS)"
echo -e "  HTTPS: https://$DOMAIN"
echo ""
echo "证书信息："
echo -e "  证书路径: /etc/letsencrypt/live/$DOMAIN/"
echo -e "  备份路径: /opt/FamilyTutorHub/ssl/"
echo ""
echo "后续操作："
echo "  1. 在 Cloudflare 设置 SSL/TLS 模式为 'Full (strict)'"
echo "  2. 更新 DNS 记录指向您的 VPS IP"
echo "  3. 清除浏览器缓存并测试"
echo ""
echo -e "${YELLOW}注意: 证书有效期为 90 天，已配置自动续期${NC}"
echo ""
