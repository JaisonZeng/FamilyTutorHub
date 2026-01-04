#!/bin/bash

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== FamilyTutorHub 部署脚本 ===${NC}"
echo ""

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}请使用 sudo 运行此脚本${NC}"
    exit 1
fi

# 检查系统信息
echo -e "${YELLOW}[1/8] 检查系统信息...${NC}"
echo "操作系统: $(lsb_release -d | cut -f2)"
echo "内核: $(uname -r)"
echo "CPU: $(nproc) 核"
echo "内存: $(free -h | awk '/^Mem:/{print $2}')"
echo ""

# 检查 Docker
echo -e "${YELLOW}[2/8] 检查 Docker...${NC}"
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker 未安装，正在安装...${NC}"

    # 更新包索引
    apt-get update

    # 安装依赖
    apt-get install -y \
        ca-certificates \
        curl \
        gnupg \
        lsb-release

    # 添加 Docker 官方 GPG key
    mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg

    # 设置 Docker 仓库
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

    # 安装 Docker
    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

    echo -e "${GREEN}Docker 安装完成${NC}"
else
    echo -e "${GREEN}Docker 已安装: $(docker --version)${NC}"
fi

# 启动 Docker
systemctl start docker
systemctl enable docker

# 检查 Docker Compose
echo ""
echo -e "${YELLOW}[3/8] 检查 Docker Compose...${NC}"
if ! docker compose version &> /dev/null; then
    echo -e "${RED}Docker Compose 未安装，正在安装...${NC}"
    apt-get install -y docker-compose
fi
echo -e "${GREEN}Docker Compose: $(docker compose version)${NC}"

# 配置防火墙
echo ""
echo -e "${YELLOW}[4/8] 配置防火墙...${NC}"
if command -v ufw &> /dev/null; then
    echo "允许 HTTP (80)..."
    ufw allow 80/tcp

    echo "允许 HTTPS (443)..."
    ufw allow 443/tcp

    echo "允许 SSH (22)..."
    ufw allow 22/tcp

    echo -e "${GREEN}防火墙规则已配置${NC}"
    echo "当前状态:"
    ufw status
else
    echo -e "${YELLOW}未检测到 ufw，跳过防火墙配置${NC}"
fi

# 检查并关闭占用端口的进程
echo ""
echo -e "${YELLOW}[5/8] 检查端口占用...${NC}"
for port in 80 3306; do
    pid=$(lsof -ti:$port 2>/dev/null)
    if [ ! -z "$pid" ]; then
        echo -e "${RED}端口 $port 被进程 $pid 占用${NC}"
        read -p "是否关闭该进程? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            kill -9 $pid
            echo -e "${GREEN}进程已关闭${NC}"
        fi
    fi
done

# 创建备份目录
echo ""
echo -e "${YELLOW}[6/8] 创建备份目录...${NC}"
mkdir -p /backup/tutor
mkdir -p /var/log/tutor
echo -e "${GREEN}备份目录已创建${NC}"

# 构建并启动服务
echo ""
echo -e "${YELLOW}[7/8] 构建并启动服务...${NC}"
echo "这可能需要几分钟，请耐心等待..."

# 构建镜像
docker compose build --no-cache

# 启动服务
docker compose up -d

# 等待服务启动
echo "等待服务启动..."
sleep 30

# 验证部署
echo ""
echo -e "${YELLOW}[8/8] 验证部署...${NC}"

# 检查容器状态
echo ""
echo "容器状态:"
docker compose ps

# 检查后端健康
echo ""
echo "检查后端健康状态..."
if curl -f http://localhost:8080/health &> /dev/null; then
    echo -e "${GREEN}✓ 后端服务正常${NC}"
else
    echo -e "${RED}✗ 后端服务异常${NC}"
fi

# 检查数据库
echo ""
echo "检查数据库连接..."
if docker compose exec -T db mysqladmin ping -h localhost --silent; then
    echo -e "${GREEN}✓ 数据库连接正常${NC}"
else
    echo -e "${RED}✗ 数据库连接异常${NC}"
fi

# 完成
echo ""
echo -e "${GREEN}=== 部署完成 ===${NC}"
echo ""
echo "访问信息:"
echo "  前端: http://$(curl -s ifconfig.me)"
echo "  后端: http://$(curl -s ifconfig.me):8080"
echo "  API文档: http://$(curl -s ifconfig.me):8080/swagger"
echo ""
echo "默认账号: admin / admin123"
echo ""
echo "常用命令:"
echo "  查看日志: docker compose logs -f"
echo "  重启服务: docker compose restart"
echo "  停止服务: docker compose down"
echo "  查看状态: docker compose ps"
echo ""
echo -e "${YELLOW}重要提示:${NC}"
echo "1. 请立即修改数据库密码和管理员密码"
echo "2. 建议配置 HTTPS 证书"
echo "3. 设置定期备份任务"
echo ""
echo -e "${YELLOW}备份脚本:${NC}"
echo "文件位置: ./scripts/backup.sh"
echo "设置定时任务: crontab -e"
echo "添加: 0 2 * * * /path/to/FamilyTutorHub/scripts/backup.sh"
echo ""
