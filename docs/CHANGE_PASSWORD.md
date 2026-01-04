# 修改密码功能使用指南

## 后端 API

已添加修改密码 API：`PUT /api/password`

### API 详情

**端点**: `PUT /api/password`

**请求头**:
```
Authorization: Bearer {your_token}
Content-Type: application/json
```

**请求体**:
```json
{
  "old_password": "admin123",
  "new_password": "your_new_secure_password"
}
```

**成功响应** (200):
```json
{
  "message": "密码修改成功"
}
```

**错误响应**:
- 400: 原密码错误 或 新密码格式不正确
- 401: 未登录
- 500: 服务器错误

---

## 使用方法

### 方法 1: 通过 API 直接调用（最快）

#### 1. 获取登录 Token

```bash
curl -X POST http://your-vps-ip:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

响应会包含 token：
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "currentUser": {...}
  }
}
```

#### 2. 使用 Token 修改密码

```bash
curl -X PUT http://your-vps-ip:8080/api/password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "old_password": "admin123",
    "new_password": "your_new_password"
  }'
```

---

### 方法 2: 使用浏览器开发者工具

#### 步骤：

1. **登录系统**
   - 访问 http://familytutorhub.jai-squidward.top
   - 使用 admin/admin123 登录

2. **打开开发者工具**
   - 按 F12 或右键 → 检查
   - 切换到 Console (控制台) 标签

3. **获取 Token**
   ```javascript
   // 在控制台输入
   localStorage.getItem('token')
   ```

4. **调用 API 修改密码**
   ```javascript
   // 替换 YOUR_TOKEN 和 NEW_PASSWORD
   fetch('http://your-vps-ip:8080/api/password', {
     method: 'PUT',
     headers: {
       'Content-Type': 'application/json',
       'Authorization': 'Bearer ' + localStorage.getItem('token')
     },
     body: JSON.stringify({
       old_password: 'admin123',
       new_password: 'your_new_password'
     })
   }).then(r => r.json()).then(console.log)
   ```

---

### 方法 3: 使用 Swagger 文档

1. 访问 Swagger 文档：`http://your-vps-ip:8080/swagger`
2. 找到 `认证` → `PUT /api/password`
3. 点击 "Try it out"
4. 输入旧密码和新密码
5. 点击 "Execute"

---

## 部署步骤

### 1. 提交代码

```bash
git add .
git commit -m "feat: add change password API"
git push
```

### 2. 等待 CI/CD 自动部署

GitHub Actions 会自动：
- 构建后端镜像
- 部署到 VPS
- 重启后端容器

### 3. 验证部署

```bash
# 在 VPS 上检查
docker compose logs backend --tail 50
```

---

## 快速测试脚本

创建一个测试脚本 `test-change-password.sh`:

```bash
#!/bin/bash

# 配置
API_URL="http://your-vps-ip:8080"
USERNAME="admin"
OLD_PASSWORD="admin123"
NEW_PASSWORD="your_new_password"

echo "=== 登录获取 Token ==="
TOKEN=$(curl -s -X POST $API_URL/api/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$OLD_PASSWORD\"}" \
  | jq -r '.token')

echo "Token: $TOKEN"

echo ""
echo "=== 修改密码 ==="
curl -X PUT $API_URL/api/password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"old_password\":\"$OLD_PASSWORD\",\"new_password\":\"$NEW_PASSWORD\"}"
```

使用：
```bash
chmod +x test-change-password.sh
./test-change-password.sh
```

---

## 安全建议

1. **立即修改默认密码**
   - 默认: admin / admin123
   - 首次登录后立即修改

2. **使用强密码**
   - 至少 12 位
   - 包含大小写字母、数字、特殊符号
   - 不使用个人信息

3. **定期更换密码**
   - 建议 3-6 个月更换一次

4. **不要在多个网站使用相同密码**

---

## 后续开发建议

### 添加前端页面

可以在以下位置添加修改密码功能：

**选项 1: 在用户设置页面**
- 路径: `/settings` 或 `/account`
- 添加修改密码表单

**选项 2: 在右上角用户菜单**
- 点击头像 → 下拉菜单 → "修改密码"
- 弹出对话框

**选项 3: 在欢迎页面**
- 路径: `/welcome`
- 添加"首次登录请修改密码"提示

### 前端组件示例

```tsx
// src/pages/ChangePassword/index.tsx
import { Button, Form, Input, message } from 'antd';
import { changePassword } from '@/services/ant-design-pro/api';

export default () => {
  const [form] = Form.useForm();

  const handleSubmit = async (values: any) => {
    try {
      await changePassword(values);
      message.success('密码修改成功，请重新登录');
      // 退出登录或跳转
    } catch (error) {
      message.error('密码修改失败');
    }
  };

  return (
    <Form form={form} onFinish={handleSubmit}>
      <Form.Item name="old_password" label="原密码" rules={[{ required: true }]}>
        <Input.Password />
      </Form.Item>
      <Form.Item name="new_password" label="新密码" rules={[{ required: true, min: 6 }]}>
        <Input.Password />
      </Form.Item>
      <Form.Item name="confirm_password" label="确认密码" dependencies={['new_password']}>
        <Input.Password />
      </Form.Item>
      <Button type="primary" htmlType="submit">修改密码</Button>
    </Form>
  );
};
```

---

## 故障排查

### 问题：401 Unauthorized

**原因**: Token 无效或过期

**解决**: 重新登录获取新 Token

### 问题：400 原密码错误

**原因**: 输入的旧密码不正确

**解决**: 检查是否输入正确的旧密码

### 问题：500 服务器错误

**原因**: 数据库连接失败或加密失败

**解决**: 检查后端日志
```bash
docker compose logs backend
```

---

## 总结

✅ 后端 API 已完成
✅ 可以通过多种方式修改密码
⏳ 前端页面待开发

**建议**: 先使用 API 方法修改密码，确保账号安全，然后再开发前端页面。
