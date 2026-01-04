FROM golang:1.24-alpine AS builder
WORKDIR /app

# 安装必要的构建工具
RUN apk add --no-cache git

# 安装 swag 工具
RUN go install github.com/swaggo/swag/cmd/swag@latest

COPY go.mod go.sum ./
RUN go mod download
COPY . .

# 生成 Swagger 文档并编译
RUN swag init && go build -o server .

FROM alpine:latest
WORKDIR /app
# 添加时区数据
RUN apk add --no-cache tzdata
COPY --from=builder /app/server .
EXPOSE 8080
CMD ["./server"]
