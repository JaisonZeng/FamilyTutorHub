package middleware

import (
	"net/http"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
	"golang.org/x/time/rate"
)

// IPRateLimiter 存储每个 IP 的限流器
type IPRateLimiter struct {
	ips map[string]*rate.Limiter
	mu  sync.RWMutex

	// 限流配置
	rate rate.Limit // 每秒允许的请求数
	burst int       // 允许的突发请求数
}

// NewIPRateLimiter 创建 IP 限流器
func NewIPRateLimiter(r rate.Limit, b int) *IPRateLimiter {
	return &IPRateLimiter{
		ips:   make(map[string]*rate.Limiter),
		rate:  r,
		burst: b,
	}
}

// GetLimiter 获取指定 IP 的限流器
func (rl *IPRateLimiter) GetLimiter(ip string) *rate.Limiter {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	limiter, exists := rl.ips[ip]
	if !exists {
		limiter = rate.NewLimiter(rl.rate, rl.burst)
		rl.ips[ip] = limiter
	}

	return limiter
}

// 清理过期的限流器
func (rl *IPRateLimiter) Cleanup() {
	ticker := time.NewTicker(5 * time.Minute)
	go func() {
		for range ticker.C {
			rl.mu.Lock()
			// 清理所有限流器，重新开始
			rl.ips = make(map[string]*rate.Limiter)
			rl.mu.Unlock()
		}
	}()
}

// 全局限流器实例
var (
	globalLimiter *IPRateLimiter
	apiLimiter    *IPRateLimiter
)

// InitRateLimiters 初始化限流器
func InitRateLimiters() {
	// 全局限流：每秒 30 个请求，突发 50 个
	globalLimiter = NewIPRateLimiter(30, 50)
	globalLimiter.Cleanup()

	// API 限流：每秒 10 个请求，突发 15 个
	apiLimiter = NewIPRateLimiter(10, 15)
	apiLimiter.Cleanup()
}

// RateLimitMiddleware 全局限流中间件
func RateLimitMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		ip := c.ClientIP()
		limiter := globalLimiter.GetLimiter(ip)

		if !limiter.Allow() {
			c.JSON(http.StatusTooManyRequests, gin.H{
				"error": "请求过于频繁，请稍后再试",
				"code":  429,
			})
			c.Abort()
			return
		}

		c.Next()
	}
}

// APIRateLimitMiddleware API 限流中间件
func APIRateLimitMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		ip := c.ClientIP()
		limiter := apiLimiter.GetLimiter(ip)

		if !limiter.Allow() {
			c.JSON(http.StatusTooManyRequests, gin.H{
				"error": "API 请求过于频繁，请稍后再试",
				"code":  429,
			})
			c.Abort()
			return
		}

		c.Next()
	}
}
