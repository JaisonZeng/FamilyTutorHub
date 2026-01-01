package handlers

import (
	"net/http"

	"tutor-management/models"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

type StudentHandler struct {
	DB *gorm.DB
}

func NewStudentHandler(db *gorm.DB) *StudentHandler {
	return &StudentHandler{DB: db}
}

// GetAll 获取所有学生
// @Summary 获取所有学生
// @Tags 学生管理
// @Security BearerAuth
// @Produce json
// @Success 200 {array} models.Student
// @Router /students [get]
func (h *StudentHandler) GetAll(c *gin.Context) {
	var students []models.Student
	if err := h.DB.Find(&students).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, students)
}

// Create 创建学生
// @Summary 创建学生
// @Tags 学生管理
// @Security BearerAuth
// @Accept json
// @Produce json
// @Param student body models.Student true "学生信息"
// @Success 201 {object} models.Student
// @Failure 400 {object} map[string]string
// @Router /students [post]
func (h *StudentHandler) Create(c *gin.Context) {
	var student models.Student
	if err := c.ShouldBindJSON(&student); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if err := h.DB.Create(&student).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, student)
}

// Update 更新学生
// @Summary 更新学生信息
// @Tags 学生管理
// @Security BearerAuth
// @Accept json
// @Produce json
// @Param id path int true "学生ID"
// @Param student body models.Student true "学生信息"
// @Success 200 {object} models.Student
// @Failure 404 {object} map[string]string
// @Router /students/{id} [put]
func (h *StudentHandler) Update(c *gin.Context) {
	id := c.Param("id")
	var student models.Student
	if err := h.DB.First(&student, id).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "学生不存在"})
		return
	}
	if err := c.ShouldBindJSON(&student); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if err := h.DB.Save(&student).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, student)
}

// Delete 删除学生
// @Summary 删除学生
// @Tags 学生管理
// @Security BearerAuth
// @Param id path int true "学生ID"
// @Success 200 {object} map[string]string
// @Router /students/{id} [delete]
func (h *StudentHandler) Delete(c *gin.Context) {
	id := c.Param("id")
	if err := h.DB.Delete(&models.Student{}, id).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "删除成功"})
}
