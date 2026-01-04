# 修改默认密码指南

## ⚠️ 重要安全警告

**默认密码仅用于首次设置，必须在生产环境中修改！**

- 默认管理员账号: `admin / admin123`
- 默认数据库密码: `root123` / `tutor123`

---

## 方法 1: 通过 Web 界面修改（推荐）

### 修改管理员密码

1. 访问您的网站: `http://your-vps-ip`
2. 使用 `admin / admin123` 登录
3. 点击右上角头像 → 设置
4. 修改密码

---

## 方法 2: 通过 API 修改

### 1. 登录获取 Token

```bash
curl -X POST http://your-vps-ip:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

响应：
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

### 2. 使用 Token 修改密码

```bash
curl -X PUT http://your-vps-ip:8080/api/auth/password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "old_password": "admin123",
    "new_password": "your_new_secure_password"
  }'
```

---

## 方法 3: 修改数据库密码

### 使用脚本（简单）

在 VPS 上执行：

```bash
cd /opt/FamilyTutorHub
chmod +x scripts/change-password.sh
./scripts/change-password.sh
```

### 手动修改

1. **编辑 docker-compose.yml**:

```bash
nano docker-compose.yml
```

2. **修改以下行**:

```yaml
services:
  db:
    environment:
      MYSQL_ROOT_PASSWORD: YOUR_NEW_ROOT_PASSWORD  # 修改这里
      MYSQL_PASSWORD: YOUR_NEW_TUTOR_PASSWORD      # 修改这里

  backend:
    environment:
      DB_PASSWORD: YOUR_NEW_TUTOR_PASSWORD        # 修改这里（与上面一致）
```

3. **重启服务**:

```bash
docker compose down
docker compose up -d
```

---

## 方法 4: 使用环境变量（最安全）

### 1. 创建 .env 文件

```bash
cd /opt/FamilyTutorHub
nano .env
```

### 2. 添加以下内容

```env
# 数据库配置
DB_ROOT_PASSWORD=your_secure_root_password_here
DB_PASSWORD=your_secure_tutor_password_here

# 管理员初始密码（首次启动后立即修改）
ADMIN_PASSWORD=your_secure_admin_password_here
```

### 3. 修改 docker-compose.yml 使用环境变量

```yaml
services:
  db:
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_PASSWORD: ${DB_PASSWORD}

  backend:
    environment:
      DB_PASSWORD: ${DB_PASSWORD}
```

### 4. 确保 .env 不被提交到 Git

```bash
# .gitignore 已包含 .env
```

---

## 密码安全建议

### ✅ 好密码的特征

- 至少 12 个字符
- 包含大小写字母、数字、特殊符号
- 不包含个人信息
- 不使用字典词汇

### 🔐 生成强密码

```bash
# 方法 1: OpenSSL
openssl rand -base64 16

# 方法 2: 使用脚本
cd /opt/FamilyTutorHub/scripts
./security-tools.sh
```

### 📝 密码管理

- 使用密码管理器（如 1Password、Bitwarden）
- 不要在不同网站使用相同密码
- 定期更换密码（建议 3-6 个月）
- 启用双因素认证（如果支持）

---

## 验证密码修改

### 检查数据库密码

```bash
docker compose exec db mysql -u tutor -p
# 输入新密码
```

### 检查管理员密码

尝试使用新密码登录 Web 界面。

---

## 如果忘记密码

### 重置管理员密码

```bash
# 连接到数据库
docker compose exec db mysql -u root -p

use tutor;
-- 删除现有管理员
DELETE FROM users WHERE username = 'admin';
-- 退出后重启后端，会自动创建新的 admin/admin123 账号
docker compose restart backend
```

### 重置数据库密码

```bash
# 停止服务
docker compose down

# 删除数据库卷（⚠️ 会丢失所有数据）
docker volume rm familytutorhub_mysqldata

# 修改 docker-compose.yml 中的密码
nano docker-compose.yml

# 重新启动（会创建新的数据库）
docker compose up -d
```

---

## 常见问题

**Q: 修改密码后无法登录？**

A: 检查以下几点：
1. 密码是否正确（区分大小写）
2. 前后端是否都重启了
3. 清除浏览器缓存

**Q: 数据库密码修改后后端连接失败？**

A: 确保 docker-compose.yml 中的三个密码位置都已修改：
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_PASSWORD`
- `DB_PASSWORD`（必须与 MYSQL_PASSWORD 一致）

**Q: 如何批量修改所有密码？**

A: 使用脚本：
```bash
cd /opt/FamilyTutorHub
./scripts/change-password.sh
# 选择选项 3
```
