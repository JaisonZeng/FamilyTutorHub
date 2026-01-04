#!/bin/bash

# 安全管理工具脚本
# 用于管理 IP 黑名单、查看攻击日志等

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

COMPOSE_DIR="/opt/FamilyTutorHub"

show_menu() {
    echo ""
    echo -e "${GREEN}=== 安全管理工具 ===${NC}"
    echo "1. 查看当前黑名单"
    echo "2. 添加 IP 到黑名单"
    echo "3. 从黑名单移除 IP"
    echo "4. 查看最近的错误日志"
    echo "5. 查看最近的被拒绝请求"
    echo "6. 查看访问量最高的 IP"
    echo "7. 查看系统资源使用"
    echo "8. 一键封禁可疑 IP (多次失败登录)"
    echo "0. 退出"
    echo ""
}

view_blacklist() {
    echo -e "${YELLOW}当前黑名单:${NC}"
    docker compose -f $COMPOSE_DIR/docker-compose.yml logs backend 2>&1 | grep -i "blacklist\|blocked" || echo "没有黑名单记录"
}

add_to_blacklist() {
    read -p "输入要封禁的 IP: " ip
    echo "$ip" | docker compose -f $COMPOSE_DIR/docker-compose.yml exec -T backend tee /tmp/blacklist.txt > /dev/null
    echo -e "${GREEN}IP $ip 已添加到黑名单${NC}"
    echo "注意: 需要重启 backend 服务才能生效"
}

remove_from_blacklist() {
    read -p "输入要从黑名单移除的 IP: " ip
    echo -e "${YELLOW}请手动从代码或配置中移除 $ip${NC}"
}

view_error_logs() {
    echo -e "${YELLOW}最近的错误日志 (最近 50 条):${NC}"
    docker compose -f $COMPOSE_DIR/docker-compose.yml logs --tail=50 backend 2>&1 | grep -i "error\|panic\|fatal"
}

view_rejected_requests() {
    echo -e "${YELLOW}最近被拒绝的请求:${NC}"
    docker compose -f $COMPOSE_DIR/docker-compose.yml logs --tail=100 backend 2>&1 | grep -i "429\|403\|rate limit"
}

view_top_ips() {
    echo -e "${YELLOW}访问量最高的 IP (前 10):${NC}"
    docker compose -f $COMPOSE_DIR/docker-compose.yml logs backend 2>&1 | grep -oE '([0-9]{1,3}\.){3}[0-9]{1,3}' | sort | uniq -c | sort -rn | head -10
}

view_resources() {
    echo -e "${YELLOW}系统资源使用:${NC}"
    echo ""
    echo "Docker 容器资源使用:"
    docker stats --no-stream
    echo ""
    echo "系统资源:"
    echo "CPU: $(nproc) 核"
    echo "内存: $(free -h | awk '/^Mem:/{print $3 "/" $2}')"
    echo "磁盘: $(df -h / | awk 'NR==2{print $3 "/" $2 " (" $5 ")"}')"
}

block_suspicious_ips() {
    echo -e "${YELLOW}检测多次失败登录的 IP...${NC}"
    docker compose -f $COMPOSE_DIR/docker-compose.yml logs backend 2>&1 | grep -i "failed login\|invalid credentials" | grep -oE '([0-9]{1,3}\.){3}[0-9]{1,3}' | sort | uniq -c | sort -rn | head -10
    echo ""
    echo "以上是出现多次失败登录的 IP"
    read -p "是否要批量封禁这些 IP? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "请使用选项 2 逐个添加，或修改代码实现批量封禁"
    fi
}

while true; do
    show_menu
    read -p "请选择操作: " choice

    case $choice in
        1) view_blacklist ;;
        2) add_to_blacklist ;;
        3) remove_from_blacklist ;;
        4) view_error_logs ;;
        5) view_rejected_requests ;;
        6) view_top_ips ;;
        7) view_resources ;;
        8) block_suspicious_ips ;;
        0) echo "退出"; break ;;
        *) echo -e "${RED}无效选择${NC}" ;;
    esac
done
