package middleware

import (
	"time"

	"tutor-management/utils"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

// RequestLogger 请求日志中间件
func RequestLogger() gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		path := c.Request.URL.Path
		query := c.Request.URL.RawQuery

		// 处理请求
		c.Next()

		// 计算耗时
		latency := time.Since(start)

		// 记录日志
		fields := []zap.Field{
			zap.Int("status", c.Writer.Status()),
			zap.String("method", c.Request.Method),
			zap.String("path", path),
			zap.String("query", query),
			zap.String("ip", c.ClientIP()),
			zap.String("user-agent", c.Request.UserAgent()),
			zap.Duration("latency", latency),
			zap.Int("body_size", c.Writer.Size()),
		}

		// 如果有错误，添加错误信息
		if len(c.Errors) > 0 {
			fields = append(fields, zap.String("errors", c.Errors.String()))
		}

		// 根据状态码选择日志级别
		if c.Writer.Status() >= 500 {
			utils.Error("Server error", fields...)
		} else if c.Writer.Status() >= 400 {
			utils.Warn("Client error", fields...)
		} else {
			utils.Info("Request completed", fields...)
		}
	}
}

// SlowRequestLogger 慢请求日志中间件
func SlowRequestLogger(threshold time.Duration) gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()

		c.Next()

		latency := time.Since(start)
		if latency > threshold {
			utils.Warn("Slow request detected",
				zap.String("method", c.Request.Method),
				zap.String("path", c.Request.URL.Path),
				zap.Duration("latency", latency),
				zap.Duration("threshold", threshold),
				zap.String("ip", c.ClientIP()),
			)
		}
	}
}
