package middleware

import (
	"github.com/gin-gonic/gin"
)

// SecurityMiddleware 安全头中间件
func SecurityMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		// 防止点击劫持
		c.Header("X-Frame-Options", "DENY")

		// 防止 MIME 类型嗅探
		c.Header("X-Content-Type-Options", "nosniff")

		// 启用 XSS 保护
		c.Header("X-XSS-Protection", "1; mode=block")

		// 限制引用来源
		c.Header("Referrer-Policy", "strict-origin-when-cross-origin")

		// 内容安全策略
		c.Header("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'")

		// HSTS (仅在 HTTPS 时启用)
		// c.Header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")

		// 隐藏服务器信息
		c.Header("Server", "")

		c.Next()
	}
}

// CORSMiddleware CORS 中间件（增强版）
func CORSMiddleware(allowedOrigins []string) gin.HandlerFunc {
	return func(c *gin.Context) {
		origin := c.Request.Header.Get("Origin")

		// 检查 origin 是否在允许列表中
		allowed := false
		for _, allowedOrigin := range allowedOrigins {
			if allowedOrigin == "*" || allowedOrigin == origin {
				allowed = true
				c.Header("Access-Control-Allow-Origin", origin)
				break
			}
		}

		if !allowed && len(allowedOrigins) > 0 {
			// 如果没有匹配且不允许所有来源，则不设置 CORS 头
			c.Next()
			return
		} else if len(allowedOrigins) == 0 {
			c.Header("Access-Control-Allow-Origin", "*")
		}

		c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization, X-Requested-With")
		c.Header("Access-Control-Expose-Headers", "Content-Length, Content-Type")
		c.Header("Access-Control-Max-Age", "86400")

		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}

		c.Next()
	}
}
