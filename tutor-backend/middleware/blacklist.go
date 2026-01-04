package middleware

import (
	"net"
	"sync"

	"github.com/gin-gonic/gin"
)

// IPBlacklist IP 黑名单管理器
type IPBlacklist struct {
	blacklist map[string]bool
	mu        sync.RWMutex
}

var blacklistManager *IPBlacklist

// InitBlacklist 初始化黑名单
func InitBlacklist() {
	blacklistManager = &IPBlacklist{
		blacklist: make(map[string]bool),
	}
}

// AddIP 添加 IP 到黑名单
func (b *IPBlacklist) AddIP(ip string) {
	b.mu.Lock()
	defer b.mu.Unlock()
	b.blacklist[ip] = true
}

// RemoveIP 从黑名单移除 IP
func (b *IPBlacklist) RemoveIP(ip string) {
	b.mu.Lock()
	defer b.mu.Unlock()
	delete(b.blacklist, ip)
}

// IsBlocked 检查 IP 是否被封锁
func (b *IPBlacklist) IsBlocked(ip string) bool {
	b.mu.RLock()
	defer b.mu.RUnlock()

	// 精确匹配
	if b.blacklist[ip] {
		return true
	}

	// CIDR 匹配
	parsedIP := net.ParseIP(ip)
	if parsedIP != nil {
		for blockedIP := range b.blacklist {
			// 检查是否是 CIDR 格式
			if containsCIDR(blockedIP) {
				_, ipNet, err := net.ParseCIDR(blockedIP)
				if err == nil && ipNet.Contains(parsedIP) {
					return true
				}
			}
		}
	}

	return false
}

// ListBlacklist 列出所有被封锁的 IP
func (b *IPBlacklist) ListBlacklist() []string {
	b.mu.RLock()
	defer b.mu.RUnlock()

	result := make([]string, 0, len(b.blacklist))
	for ip := range b.blacklist {
		result = append(result, ip)
	}
	return result
}

// containsCIDR 检查字符串是否包含 CIDR 表示法
func containsCIDR(s string) bool {
	for i := 0; i < len(s); i++ {
		if s[i] == '/' {
			return true
		}
	}
	return false
}

// BlacklistMiddleware 黑名单中间件
func BlacklistMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		ip := c.ClientIP()

		if blacklistManager.IsBlocked(ip) {
			c.JSON(403, gin.H{
				"error": "您的 IP 已被禁止访问",
				"code":  403,
			})
			c.Abort()
			return
		}

		c.Next()
	}
}

// GetBlacklistManager 获取黑名单管理器实例
func GetBlacklistManager() *IPBlacklist {
	return blacklistManager
}
