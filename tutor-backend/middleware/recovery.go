package middleware

import (
	"fmt"
	"net/http"
	"runtime/debug"

	"tutor-management/utils"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
)

// Recovery 错误恢复中间件
func Recovery() gin.HandlerFunc {
	return func(c *gin.Context) {
		defer func() {
			if err := recover(); err != nil {
				// 获取堆栈信息
				stack := string(debug.Stack())

				// 记录panic日志
				utils.Error("Panic recovered",
					zap.Any("error", err),
					zap.String("stack", stack),
					zap.String("method", c.Request.Method),
					zap.String("path", c.Request.URL.Path),
					zap.String("ip", c.ClientIP()),
				)

				// 返回500错误
				c.JSON(http.StatusInternalServerError, gin.H{
					"error": "Internal server error",
					"msg":   fmt.Sprintf("%v", err),
				})
				c.Abort()
			}
		}()

		c.Next()
	}
}
