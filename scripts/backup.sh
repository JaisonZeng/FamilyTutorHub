#!/bin/bash

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 配置
BACKUP_DIR="/backup/tutor"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30
COMPOSE_DIR="/opt/FamilyTutorHub"

# 创建备份目录
mkdir -p $BACKUP_DIR

echo -e "${GREEN}=== 开始备份 ===${NC}"
echo "时间: $(date)"
echo ""

# 备份数据库
echo -e "${YELLOW}备份数据库...${NC}"
docker compose -f $COMPOSE_DIR/docker-compose.yml exec -T db mysqldump \
    -u tutor \
    -ptutor123 \
    --single-transaction \
    --routines \
    --triggers \
    tutor > $BACKUP_DIR/db_$DATE.sql

if [ $? -eq 0 ]; then
    # 压缩备份
    gzip $BACKUP_DIR/db_$DATE.sql
    echo -e "${GREEN}数据库备份完成: $BACKUP_DIR/db_$DATE.sql.gz${NC}"

    # 显示备份大小
    SIZE=$(du -h $BACKUP_DIR/db_$DATE.sql.gz | cut -f1)
    echo "备份大小: $SIZE"
else
    echo -e "${RED}数据库备份失败${NC}"
    exit 1
fi

# 删除旧备份
echo ""
echo -e "${YELLOW}清理旧备份 ($RETENTION_DAYS 天前)...${NC}"
find $BACKUP_DIR -name "db_*.sql.gz" -mtime +$RETENTION_DAYS -delete

# 显示当前备份列表
echo ""
echo "当前备份列表:"
ls -lh $BACKUP_DIR/*.sql.gz 2>/dev/null | awk '{print $9, $5}'

echo ""
echo -e "${GREEN}=== 备份完成 ===${NC}"
