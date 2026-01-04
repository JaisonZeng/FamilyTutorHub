#!/bin/bash

# 密码修改脚本
# 用途：修改数据库和管理员密码

set -e

echo "==================================="
echo "  FamilyTutorHub 密码修改工具"
echo "==================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 生成随机密码
generate_password() {
    openssl rand -base64 16 | tr -d '/+='
}

echo "请选择要修改的密码："
echo "1) 数据库密码 (MySQL root 和 tutor 用户)"
echo "2) 管理员密码 (Web 登录)"
echo "3) 全部修改"
echo ""
read -p "请输入选项 (1-3): " choice

case $choice in
    1)
        echo -e "${YELLOW}修改数据库密码...${NC}"
        read -sp "请输入新的数据库 root 密码: " new_root_pass
        echo ""
        read -sp "请输入新的数据库 tutor 用户密码: " new_tutor_pass
        echo ""

        # 更新 docker-compose.yml
        sed -i "s/MYSQL_ROOT_PASSWORD: .*/MYSQL_ROOT_PASSWORD: $new_root_pass/" docker-compose.yml
        sed -i "s/MYSQL_PASSWORD: .*/MYSQL_PASSWORD: $new_tutor_pass/" docker-compose.yml
        sed -i "s/DB_PASSWORD: .*/DB_PASSWORD: $new_tutor_pass/" docker-compose.yml

        echo -e "${GREEN}✓ 数据库密码已更新${NC}"
        echo -e "${YELLOW}请重启容器使更改生效: docker compose restart db backend${NC}"
        ;;

    2)
        echo -e "${YELLOW}修改管理员密码...${NC}"
        echo ""
        echo "请登录系统后在用户设置中修改密码"
        echo "或者使用以下方法："
        echo ""
        echo "1. 访问: http://your-vps-ip:8080/swagger"
        echo "2. 找到 POST /api/auth/login 接口"
        echo "3. 使用 admin/admin123 登录获取 token"
        echo "4. 找到 PUT /api/auth/password 接口"
        echo "5. 使用 token 修改密码"
        echo ""
        echo -e "${RED}注意: 直接修改代码 hardcode 的密码不安全！${NC}"
        echo -e "${YELLOW}建议在首次登录后立即通过界面修改密码${NC}"
        ;;

    3)
        echo -e "${YELLOW}修改所有密码...${NC}"

        # 生成随机密码
        new_root_pass=$(generate_password)
        new_tutor_pass=$(generate_password)

        echo ""
        echo "生成的密码："
        echo -e "${GREEN}Root 密码: $new_root_pass${NC}"
        echo -e "${GREEN}Tutor 密码: $new_tutor_pass${NC}"
        echo ""

        read -p "确认使用这些密码? (y/n): " confirm
        if [ "$confirm" = "y" ]; then
            # 更新 docker-compose.yml
            sed -i "s/MYSQL_ROOT_PASSWORD: .*/MYSQL_ROOT_PASSWORD: $new_root_pass/" docker-compose.yml
            sed -i "s/MYSQL_PASSWORD: .*/MYSQL_PASSWORD: $new_tutor_pass/" docker-compose.yml
            sed -i "s/DB_PASSWORD: .*/DB_PASSWORD: $new_tutor_pass/" docker-compose.yml

            echo -e "${GREEN}✓ 数据库密码已更新${NC}"
            echo -e "${YELLOW}请重启容器: docker compose restart db backend${NC}"
            echo ""
            echo "管理员密码请通过 Web 界面修改"
        fi
        ;;

    *)
        echo -e "${RED}无效选项${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}操作完成！${NC}"
echo ""
echo "重要提示："
echo "1. 请妥善保管新密码"
echo "2. 建议使用密码管理器存储"
echo "3. 定期更换密码"
