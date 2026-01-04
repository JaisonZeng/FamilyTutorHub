# 备份与恢复指南

## 备份策略

### 备份内容

需要备份的数据包括:
1. **数据库**: 所有业务数据
2. **日志文件**: 用于审计和故障排查
3. **配置文件**: 环境配置和系统配置
4. **上传文件**: 用户上传的文件 (如有)

### 备份频率

| 数据类型 | 备份频率 | 保留时间 |
|---------|---------|---------|
| 数据库 | 每天 | 30天 |
| 日志文件 | 每周 | 90天 |
| 配置文件 | 每次修改后 | 永久 |
| 完整备份 | 每周 | 12周 |

## 数据库备份

### 自动备份脚本

创建备份脚本 `/usr/local/bin/backup-database.sh`:

```bash
#!/bin/bash

# 配置
BACKUP_DIR="/backup/tutor/database"
RETENTION_DAYS=30
DATE=$(date +%Y%m%d_%H%M%S)
LOG_FILE="/var/log/tutor-backup.log"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 记录开始时间
echo "[$(date)] Starting database backup..." >> $LOG_FILE

# 备份数据库
if docker-compose exec -T db mysqldump \
    -u tutor \
    -ptutor123 \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    tutor > $BACKUP_DIR/tutor_$DATE.sql; then
    
    # 压缩备份文件
    gzip $BACKUP_DIR/tutor_$DATE.sql
    
    # 记录成功
    echo "[$(date)] Backup completed: tutor_$DATE.sql.gz" >> $LOG_FILE
    
    # 删除旧备份
    find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete
    echo "[$(date)] Old backups cleaned (older than $RETENTION_DAYS days)" >> $LOG_FILE
else
    # 记录失败
    echo "[$(date)] ERROR: Backup failed!" >> $LOG_FILE
    exit 1
fi

# 验证备份文件
BACKUP_FILE="$BACKUP_DIR/tutor_$DATE.sql.gz"
if [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
    SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "[$(date)] Backup file size: $SIZE" >> $LOG_FILE
else
    echo "[$(date)] ERROR: Backup file is empty or missing!" >> $LOG_FILE
    exit 1
fi

echo "[$(date)] Backup process completed successfully" >> $LOG_FILE
```

### 设置自动备份

```bash
# 赋予执行权限
chmod +x /usr/local/bin/backup-database.sh

# 添加到crontab
crontab -e

# 每天凌晨2点执行备份
0 2 * * * /usr/local/bin/backup-database.sh

# 每周日凌晨3点执行完整备份
0 3 * * 0 /usr/local/bin/backup-full.sh
```

### 手动备份

```bash
# Docker环境
docker-compose exec -T db mysqldump \
    -u tutor \
    -ptutor123 \
    --single-transaction \
    tutor > backup_$(date +%Y%m%d).sql

# 压缩
gzip backup_$(date +%Y%m%d).sql

# 本地MySQL
mysqldump -u tutor -ptutor123 \
    --single-transaction \
    tutor > backup_$(date +%Y%m%d).sql
```

### 备份验证

```bash
# 检查备份文件
ls -lh /backup/tutor/database/

# 验证备份完整性
gunzip -t /backup/tutor/database/tutor_20260103.sql.gz

# 查看备份内容
gunzip -c /backup/tutor/database/tutor_20260103.sql.gz | head -20
```

---

## 数据库恢复

### 完整恢复

```bash
# 1. 停止后端服务
docker-compose stop backend

# 2. 解压备份文件
gunzip /backup/tutor/database/tutor_20260103.sql.gz

# 3. 恢复数据库
docker-compose exec -T db mysql -u tutor -ptutor123 tutor < /backup/tutor/database/tutor_20260103.sql

# 4. 验证数据
docker-compose exec db mysql -u tutor -ptutor123 tutor -e "SELECT COUNT(*) FROM students"

# 5. 重启后端服务
docker-compose start backend

# 6. 验证服务
curl http://localhost:8080/health/ready
```

### 恢复到新数据库

```bash
# 1. 创建新数据库
docker-compose exec db mysql -u root -proot123 -e "CREATE DATABASE tutor_restore"

# 2. 恢复数据
docker-compose exec -T db mysql -u root -proot123 tutor_restore < backup_20260103.sql

# 3. 验证数据
docker-compose exec db mysql -u root -proot123 tutor_restore -e "SHOW TABLES"
```

### 部分恢复 (单表)

```bash
# 1. 从备份中提取单表
gunzip -c backup_20260103.sql.gz | sed -n '/CREATE TABLE `students`/,/UNLOCK TABLES/p' > students_only.sql

# 2. 恢复单表
docker-compose exec -T db mysql -u tutor -ptutor123 tutor < students_only.sql
```

---

## 日志备份

### 备份日志文件

```bash
#!/bin/bash
# /usr/local/bin/backup-logs.sh

BACKUP_DIR="/backup/tutor/logs"
DATE=$(date +%Y%m%d)

mkdir -p $BACKUP_DIR

# 备份Docker日志
docker-compose logs --no-color > $BACKUP_DIR/docker_logs_$DATE.log

# 备份应用日志
docker cp $(docker-compose ps -q backend):/app/logs $BACKUP_DIR/app_logs_$DATE

# 压缩
tar czf $BACKUP_DIR/logs_$DATE.tar.gz $BACKUP_DIR/*_$DATE*
rm -rf $BACKUP_DIR/*_$DATE.log $BACKUP_DIR/app_logs_$DATE

# 删除90天前的日志备份
find $BACKUP_DIR -name "*.tar.gz" -mtime +90 -delete
```

### 恢复日志

```bash
# 解压日志备份
tar xzf /backup/tutor/logs/logs_20260103.tar.gz -C /tmp/

# 查看日志
cat /tmp/docker_logs_20260103.log
```

---

## 配置文件备份

### 备份配置

```bash
#!/bin/bash
# /usr/local/bin/backup-config.sh

BACKUP_DIR="/backup/tutor/config"
DATE=$(date +%Y%m%d_%H%M%S)
PROJECT_DIR="/path/to/FamilyTutorHub"

mkdir -p $BACKUP_DIR

# 备份配置文件
tar czf $BACKUP_DIR/config_$DATE.tar.gz \
    $PROJECT_DIR/docker-compose.yml \
    $PROJECT_DIR/.env \
    $PROJECT_DIR/tutor-backend/config/ \
    $PROJECT_DIR/tutor-admin/.env.production

echo "Config backup completed: config_$DATE.tar.gz"
```

### 恢复配置

```bash
# 解压配置备份
tar xzf /backup/tutor/config/config_20260103.tar.gz -C /tmp/

# 复制配置文件
cp /tmp/docker-compose.yml /path/to/FamilyTutorHub/
cp /tmp/.env /path/to/FamilyTutorHub/
```

---

## 完整系统备份

### 备份脚本

```bash
#!/bin/bash
# /usr/local/bin/backup-full.sh

BACKUP_ROOT="/backup/tutor"
DATE=$(date +%Y%m%d_%H%M%S)
FULL_BACKUP_DIR="$BACKUP_ROOT/full/backup_$DATE"

mkdir -p $FULL_BACKUP_DIR

echo "Starting full system backup..."

# 1. 备份数据库
echo "Backing up database..."
docker-compose exec -T db mysqldump -u tutor -ptutor123 tutor | gzip > $FULL_BACKUP_DIR/database.sql.gz

# 2. 备份日志
echo "Backing up logs..."
docker cp $(docker-compose ps -q backend):/app/logs $FULL_BACKUP_DIR/logs

# 3. 备份配置
echo "Backing up configuration..."
cp docker-compose.yml $FULL_BACKUP_DIR/
cp .env $FULL_BACKUP_DIR/ 2>/dev/null || true

# 4. 备份数据卷
echo "Backing up volumes..."
docker run --rm -v familytutorhub_backend-logs:/data -v $FULL_BACKUP_DIR:/backup alpine tar czf /backup/volumes.tar.gz /data

# 5. 创建备份清单
echo "Creating backup manifest..."
cat > $FULL_BACKUP_DIR/MANIFEST.txt << EOF
Backup Date: $(date)
Backup Type: Full System Backup
Contents:
- database.sql.gz: MySQL database dump
- logs/: Application logs
- docker-compose.yml: Docker configuration
- .env: Environment variables
- volumes.tar.gz: Docker volumes

To restore:
1. Restore database: gunzip -c database.sql.gz | docker-compose exec -T db mysql -u tutor -ptutor123 tutor
2. Restore configuration: cp docker-compose.yml .env /path/to/project/
3. Restore volumes: tar xzf volumes.tar.gz
4. Restart services: docker-compose up -d
EOF

# 6. 压缩整个备份
cd $BACKUP_ROOT/full
tar czf backup_$DATE.tar.gz backup_$DATE/
rm -rf backup_$DATE/

# 7. 删除旧备份 (保留12周)
find $BACKUP_ROOT/full -name "backup_*.tar.gz" -mtime +84 -delete

echo "Full backup completed: backup_$DATE.tar.gz"
echo "Backup size: $(du -h $BACKUP_ROOT/full/backup_$DATE.tar.gz | cut -f1)"
```

### 完整恢复

```bash
#!/bin/bash
# 完整系统恢复脚本

BACKUP_FILE="/backup/tutor/full/backup_20260103_020000.tar.gz"
RESTORE_DIR="/tmp/tutor_restore"

echo "Starting full system restore..."

# 1. 解压备份
mkdir -p $RESTORE_DIR
tar xzf $BACKUP_FILE -C $RESTORE_DIR

# 2. 停止服务
docker-compose down

# 3. 恢复配置
cp $RESTORE_DIR/backup_*/docker-compose.yml .
cp $RESTORE_DIR/backup_*/.env . 2>/dev/null || true

# 4. 启动数据库
docker-compose up -d db
sleep 10

# 5. 恢复数据库
gunzip -c $RESTORE_DIR/backup_*/database.sql.gz | \
    docker-compose exec -T db mysql -u tutor -ptutor123 tutor

# 6. 恢复卷
tar xzf $RESTORE_DIR/backup_*/volumes.tar.gz -C /

# 7. 启动所有服务
docker-compose up -d

# 8. 验证
sleep 10
curl http://localhost:8080/health/ready

echo "Full system restore completed!"
```

---

## 灾难恢复计划

### 恢复时间目标 (RTO)

- **数据库恢复**: < 30分钟
- **完整系统恢复**: < 2小时

### 恢复点目标 (RPO)

- **数据库**: 最多丢失24小时数据
- **日志**: 最多丢失7天数据

### 灾难恢复步骤

1. **评估损坏程度**
   ```bash
   docker-compose ps
   docker-compose logs
   ```

2. **确定恢复点**
   ```bash
   ls -lh /backup/tutor/database/
   ```

3. **执行恢复**
   ```bash
   # 使用最新备份
   /usr/local/bin/restore-database.sh
   ```

4. **验证数据完整性**
   ```bash
   docker-compose exec db mysql -u tutor -ptutor123 tutor -e "SELECT COUNT(*) FROM students"
   ```

5. **重启服务**
   ```bash
   docker-compose up -d
   ```

6. **测试功能**
   - 登录管理后台
   - 测试关键功能
   - 检查数据一致性

---

## 备份存储建议

### 本地备份

```bash
# 创建备份目录
mkdir -p /backup/tutor/{database,logs,config,full}

# 设置权限
chmod 700 /backup/tutor
```

### 远程备份

```bash
# 使用rsync同步到远程服务器
rsync -avz --delete /backup/tutor/ backup-server:/backup/tutor/

# 或使用云存储 (AWS S3示例)
aws s3 sync /backup/tutor/ s3://my-backup-bucket/tutor/
```

### 备份加密

```bash
# 加密备份文件
gpg --symmetric --cipher-algo AES256 backup_20260103.sql.gz

# 解密
gpg --decrypt backup_20260103.sql.gz.gpg > backup_20260103.sql.gz
```

---

## 定期备份检查

### 每周检查清单

- [ ] 验证备份脚本正常运行
- [ ] 检查备份文件大小是否正常
- [ ] 查看备份日志是否有错误
- [ ] 验证至少一个备份文件的完整性
- [ ] 检查磁盘空间是否充足

### 每月检查清单

- [ ] 执行一次恢复演练
- [ ] 验证远程备份同步正常
- [ ] 检查备份保留策略
- [ ] 更新备份文档
- [ ] 测试灾难恢复流程

---

## 备份监控

### 监控脚本

```bash
#!/bin/bash
# /usr/local/bin/monitor-backup.sh

BACKUP_DIR="/backup/tutor/database"
ALERT_EMAIL="admin@example.com"

# 检查今天的备份是否存在
TODAY=$(date +%Y%m%d)
BACKUP_COUNT=$(find $BACKUP_DIR -name "*$TODAY*.sql.gz" | wc -l)

if [ $BACKUP_COUNT -eq 0 ]; then
    echo "WARNING: No backup found for today!" | \
        mail -s "Backup Alert: Missing Today's Backup" $ALERT_EMAIL
fi

# 检查备份文件大小
LATEST_BACKUP=$(ls -t $BACKUP_DIR/*.sql.gz | head -1)
SIZE=$(stat -f%z "$LATEST_BACKUP" 2>/dev/null || stat -c%s "$LATEST_BACKUP")

if [ $SIZE -lt 1000000 ]; then  # 小于1MB
    echo "WARNING: Backup file is too small: $SIZE bytes" | \
        mail -s "Backup Alert: Suspicious Backup Size" $ALERT_EMAIL
fi
```

---

## 相关文档

- [运维指南](./OPERATIONS.md)
- [部署指南](./DEPLOYMENT.md)
- [故障排查](./TROUBLESHOOTING.md)
- [日志管理](./LOGGING.md)
