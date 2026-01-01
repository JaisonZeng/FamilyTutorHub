package handlers

import (
	"net/http"

	"tutor-management/models"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

type ExamResultHandler struct {
	DB *gorm.DB
}

func NewExamResultHandler(db *gorm.DB) *ExamResultHandler {
	return &ExamResultHandler{DB: db}
}

// GetAll 获取所有成绩
// @Summary 获取所有成绩
// @Tags 成绩管理
// @Security BearerAuth
// @Produce json
// @Param student_id query int false "学生ID"
// @Param course_id query int false "课程ID"
// @Param exam_type query string false "考试类型"
// @Success 200 {array} models.ExamResult
// @Router /exam-results [get]
func (h *ExamResultHandler) GetAll(c *gin.Context) {
	var results []models.ExamResult
	query := h.DB.Preload("Student").Preload("Course")

	// 支持按学生筛选
	if studentID := c.Query("student_id"); studentID != "" {
		query = query.Where("student_id = ?", studentID)
	}
	// 支持按科目筛选
	if courseID := c.Query("course_id"); courseID != "" {
		query = query.Where("course_id = ?", courseID)
	}
	// 支持按考试类型筛选
	if examType := c.Query("exam_type"); examType != "" {
		query = query.Where("exam_type = ?", examType)
	}

	if err := query.Order("exam_date desc").Find(&results).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, results)
}

// Create 创建成绩
// @Summary 创建成绩记录
// @Tags 成绩管理
// @Security BearerAuth
// @Accept json
// @Produce json
// @Param result body models.ExamResult true "成绩信息"
// @Success 201 {object} models.ExamResult
// @Router /exam-results [post]
func (h *ExamResultHandler) Create(c *gin.Context) {
	var result models.ExamResult
	if err := c.ShouldBindJSON(&result); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if err := h.DB.Create(&result).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	// 重新加载关联数据
	h.DB.Preload("Student").Preload("Course").First(&result, result.ID)
	c.JSON(http.StatusCreated, result)
}

// Update 更新成绩
// @Summary 更新成绩记录
// @Tags 成绩管理
// @Security BearerAuth
// @Accept json
// @Produce json
// @Param id path int true "成绩ID"
// @Param result body models.ExamResult true "成绩信息"
// @Success 200 {object} models.ExamResult
// @Failure 404 {object} map[string]string
// @Router /exam-results/{id} [put]
func (h *ExamResultHandler) Update(c *gin.Context) {
	id := c.Param("id")
	var result models.ExamResult
	if err := h.DB.First(&result, id).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "成绩记录不存在"})
		return
	}
	if err := c.ShouldBindJSON(&result); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if err := h.DB.Save(&result).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	h.DB.Preload("Student").Preload("Course").First(&result, result.ID)
	c.JSON(http.StatusOK, result)
}

// Delete 删除成绩
// @Summary 删除成绩记录
// @Tags 成绩管理
// @Security BearerAuth
// @Param id path int true "成绩ID"
// @Success 200 {object} map[string]string
// @Router /exam-results/{id} [delete]
func (h *ExamResultHandler) Delete(c *gin.Context) {
	id := c.Param("id")
	if err := h.DB.Delete(&models.ExamResult{}, id).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "删除成功"})
}

// GetByStudent 获取学生成绩（用于分析）
// @Summary 获取学生成绩
// @Tags 成绩管理
// @Security BearerAuth
// @Produce json
// @Param student_id path int true "学生ID"
// @Param course_id query int false "课程ID"
// @Success 200 {array} models.ExamResult
// @Router /exam-results/student/{student_id} [get]
func (h *ExamResultHandler) GetByStudent(c *gin.Context) {
	studentID := c.Param("student_id")
	var results []models.ExamResult

	query := h.DB.Preload("Student").Preload("Course").Where("student_id = ?", studentID)

	// 支持按科目筛选
	if courseID := c.Query("course_id"); courseID != "" {
		query = query.Where("course_id = ?", courseID)
	}

	if err := query.Order("exam_date asc").Find(&results).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, results)
}
