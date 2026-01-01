package handlers

import (
	"net/http"

	"tutor-management/models"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

type CourseHandler struct {
	DB *gorm.DB
}

func NewCourseHandler(db *gorm.DB) *CourseHandler {
	return &CourseHandler{DB: db}
}

// GetAll 获取所有课程
// @Summary 获取所有课程
// @Tags 课程管理
// @Security BearerAuth
// @Produce json
// @Success 200 {array} models.Course
// @Router /courses [get]
func (h *CourseHandler) GetAll(c *gin.Context) {
	var courses []models.Course
	if err := h.DB.Find(&courses).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, courses)
}

// Create 创建课程
// @Summary 创建课程
// @Tags 课程管理
// @Security BearerAuth
// @Accept json
// @Produce json
// @Param course body models.Course true "课程信息"
// @Success 201 {object} models.Course
// @Router /courses [post]
func (h *CourseHandler) Create(c *gin.Context) {
	var course models.Course
	if err := c.ShouldBindJSON(&course); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if err := h.DB.Create(&course).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, course)
}

// Update 更新课程
// @Summary 更新课程
// @Tags 课程管理
// @Security BearerAuth
// @Accept json
// @Produce json
// @Param id path int true "课程ID"
// @Param course body models.Course true "课程信息"
// @Success 200 {object} models.Course
// @Failure 404 {object} map[string]string
// @Router /courses/{id} [put]
func (h *CourseHandler) Update(c *gin.Context) {
	id := c.Param("id")
	var course models.Course
	if err := h.DB.First(&course, id).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "课程不存在"})
		return
	}
	if err := c.ShouldBindJSON(&course); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if err := h.DB.Save(&course).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, course)
}

// Delete 删除课程
// @Summary 删除课程
// @Tags 课程管理
// @Security BearerAuth
// @Param id path int true "课程ID"
// @Success 200 {object} map[string]string
// @Router /courses/{id} [delete]
func (h *CourseHandler) Delete(c *gin.Context) {
	id := c.Param("id")
	if err := h.DB.Delete(&models.Course{}, id).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "删除成功"})
}
