package handlers

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

type HealthHandler struct {
	DB        *gorm.DB
	StartTime time.Time
}

func NewHealthHandler(db *gorm.DB) *HealthHandler {
	return &HealthHandler{
		DB:        db,
		StartTime: time.Now(),
	}
}

// HealthCheck 基础健康检查
// @Summary 健康检查
// @Tags 监控
// @Produce json
// @Success 200 {object} map[string]interface{}
// @Router /health [get]
func (h *HealthHandler) HealthCheck(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"status": "ok",
		"time":   time.Now().Format(time.RFC3339),
	})
}

// ReadinessCheck 就绪检查
// @Summary 就绪检查
// @Tags 监控
// @Produce json
// @Success 200 {object} map[string]interface{}
// @Failure 503 {object} map[string]interface{}
// @Router /health/ready [get]
func (h *HealthHandler) ReadinessCheck(c *gin.Context) {
	// 检查数据库连接
	sqlDB, err := h.DB.DB()
	if err != nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{
			"status": "not ready",
			"error":  "database connection failed",
		})
		return
	}

	if err := sqlDB.Ping(); err != nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{
			"status": "not ready",
			"error":  "database ping failed",
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"status":   "ready",
		"database": "connected",
		"uptime":   time.Since(h.StartTime).String(),
	})
}

// MetricsCheck 基础指标
// @Summary 基础指标
// @Tags 监控
// @Produce json
// @Success 200 {object} map[string]interface{}
// @Router /metrics [get]
func (h *HealthHandler) MetricsCheck(c *gin.Context) {
	sqlDB, _ := h.DB.DB()
	stats := sqlDB.Stats()

	c.JSON(http.StatusOK, gin.H{
		"uptime": time.Since(h.StartTime).String(),
		"database": gin.H{
			"open_connections": stats.OpenConnections,
			"in_use":           stats.InUse,
			"idle":             stats.Idle,
		},
	})
}
