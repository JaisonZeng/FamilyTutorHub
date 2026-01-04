# 多阶段构建 - 构建阶段
FROM node:20-alpine AS builder

WORKDIR /app

# 安装 pnpm
RUN corepack enable && corepack prepare pnpm@latest --activate

# 复制 package 文件
COPY package.json pnpm-lock.yaml ./

# 设置 npm 镜像源 (可选，如果服务器在国内或网络慢)
RUN npm config set registry https://registry.npmmirror.com/

# 【关键修改】去掉 --frozen-lockfile
# 允许 pnpm 在容器内根据 package.json 更新 lock 文件
RUN pnpm install

# 复制源代码
COPY . .

# 构建生产版本
RUN pnpm build

# 生产阶段 - 使用 nginx 提供静态文件
FROM nginx:alpine

# 复制构建产物到 nginx
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
