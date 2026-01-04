# CI/CD 自动部署配置指南

## 工作原理

1. **您推送代码到 GitHub**
   ```
   git add .
   git commit -m "feat: 新功能"
   git push
   ```

2. **GitHub Actions 自动触发** (云端免费构建)
   - ✅ 下载依赖
   - ✅ 运行测试
   - ✅ 构建 Docker 镜像
   - ✅ 推送到 Docker Hub

3. **自动部署到您的 VPS**
   - 🔗 SSH 连接 VPS
   - 📥 拉取新镜像
   - 🔄 重启容器

## 配置步骤

### 1. Docker Hub 配置

1. 注册 [Docker Hub](https://hub.docker.com/) 账号
2. 创建访问令牌：
   - 访问 https://hub.docker.com/settings/security
   - 点击 "New Access Token"
   - 权限选择 "Read & Write"
   - 复制生成的令牌

### 2. GitHub Secrets 配置

在 GitHub 仓库中配置以下 Secrets（Settings → Secrets and variables → Actions）：

| Secret 名称 | 说明 | 示例值 |
|------------|------|--------|
| `DOCKER_USERNAME` | Docker Hub 用户名 | `your-dockerhub-username` |
| `DOCKER_PASSWORD` | Docker Hub 访问令牌 | `dckr_pat_xxxxx` |
| `VPS_HOST` | VPS IP 地址 | `123.45.67.89` |
| `VPS_USERNAME` | VPS 用户名 | `root` |
| `VPS_SSH_KEY` | SSH 私钥 | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `VPS_PORT` | SSH 端口（可选） | `22` |

### 3. 生成 SSH 密钥

在**本地**执行：

```bash
# 生成 SSH 密钥对
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/github_actions

# 复制公钥到 VPS
ssh-copy-id -i ~/.ssh/github_actions.pub root@your-vps-ip

# 复制私钥内容（用于配置 GitHub Secret）
cat ~/.ssh/github_actions
```

将私钥完整内容（包括 `-----BEGIN` 和 `-----END` 行）复制到 GitHub Secret `VPS_SSH_KEY`

### 4. 更新 deploy.yml

打开 `.github/workflows/deploy.yml`，确保镜像名称正确：

```yaml
env:
  REGISTRY: docker.io
  BACKEND_IMAGE: your-dockerhub-username/familytutorhub-backend
  FRONTEND_IMAGE: your-dockerhub-username/familytutorhub-frontend
```

### 5. VPS 配置

在 VPS 上确保：

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh

# 允许 SSH 密钥登录（如果未配置）
# 编辑 /etc/ssh/sshd_config
# 确保 PubkeyAuthentication yes

# 重启 SSH
systemctl restart sshd
```

## 使用方法

### 开发环境（本地构建）

```bash
# 在 VPS 上直接构建（慢）
docker compose up -d --build
```

### 生产环境（CI/CD）

```bash
# 在本地修改代码后
git add .
git commit -m "feat: 新功能"
git push

# GitHub Actions 会自动：
# 1. 在云端构建镜像（快！）
# 2. 推送到 Docker Hub
# 3. 部署到 VPS
```

### 查看部署进度

1. 访问 GitHub 仓库的 "Actions" 标签
2. 查看最新的 workflow 运行状态
3. 点击查看详细日志

## 优势

| 对比项 | 传统方式 | CI/CD 方式 |
|--------|----------|-----------|
| 构建位置 | VPS（弱） | GitHub 云端（强） |
| 构建速度 | 慢（5-10分钟） | 快（2-3分钟） |
| VPS 负载 | 高（100%） | 低（仅拉取镜像） |
| 自动化 | 手动执行 | 完全自动 |
| 回滚 | 困难 | 简单（切换镜像标签） |

## 故障排查

### 部署失败

1. 检查 GitHub Actions 日志
2. 验证 Secrets 是否正确配置
3. 确认 VPS SSH 连接正常

### SSH 连接失败

```bash
# 测试 SSH 连接
ssh -i ~/.ssh/github_actions root@your-vps-ip

# 检查 VPS SSH 日志
tail -f /var/log/auth.log
```

### Docker 镜像拉取失败

```bash
# 在 VPS 上手动登录 Docker Hub
docker login

# 检查镜像是否存在
docker pull your-dockerhub-username/familytutorhub-backend:latest
```

## 高级配置

### 自动回滚

如果部署失败，可以自动回滚到上一个版本：

```yaml
# 在 deploy.yml 中添加
- name: 健康检查
  run: |
    curl -f http://${{ secrets.VPS_HOST }}/health || exit 1

- name: 回滚
  if: failure()
  run: |
    # 部署上一个版本的镜像
```

### 多环境部署

可以为不同环境（开发、测试、生产）配置不同的 workflow：

```yaml
on:
  push:
    branches:
      - develop    # 开发环境
      - main       # 生产环境
```

## 成本

- **GitHub Actions**: 免费额度（公开仓库无限，私有仓库每月 2000 分钟）
- **Docker Hub**: 免费账户（1 个私有仓库，无限公共仓库）
- **总成本**: $0/月
