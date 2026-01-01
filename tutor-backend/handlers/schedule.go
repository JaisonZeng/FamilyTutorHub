package handlers

import (
	"net/http"
	"time"

	"tutor-management/models"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

type ScheduleHandler struct {
	DB *gorm.DB
}

func NewScheduleHandler(db *gorm.DB) *ScheduleHandler {
	return &ScheduleHandler{DB: db}
}

// GetAll 获取所有排课
// @Summary 获取所有排课
// @Tags 排课管理
// @Security BearerAuth
// @Produce json
// @Success 200 {array} models.Schedule
// @Router /schedules [get]
func (h *ScheduleHandler) GetAll(c *gin.Context) {
	var schedules []models.Schedule
	if err := h.DB.Preload("Student").Preload("Course").Order("start_time DESC").Find(&schedules).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, schedules)
}

// Search 搜索排课
// @Summary 搜索排课
// @Tags 排课管理
// @Security BearerAuth
// @Produce json
// @Param start_date query string false "开始日期 (yyyy-MM-dd)"
// @Param end_date query string false "结束日期 (yyyy-MM-dd)"
// @Param student_id query int false "学生ID"
// @Param course_id query int false "课程ID"
// @Param status query string false "状态"
// @Success 200 {array} models.Schedule
// @Router /schedules/search [get]
func (h *ScheduleHandler) Search(c *gin.Context) {
	var schedules []models.Schedule

	query := h.DB.Preload("Student").Preload("Course")

	// 时间范围筛选
	if startDate := c.Query("start_date"); startDate != "" {
		if t, err := time.Parse("2006-01-02", startDate); err == nil {
			query = query.Where("start_time >= ?", t)
		}
	}
	if endDate := c.Query("end_date"); endDate != "" {
		if t, err := time.Parse("2006-01-02", endDate); err == nil {
			// 结束日期加一天，包含当天
			query = query.Where("start_time < ?", t.Add(24*time.Hour))
		}
	}

	// 学生筛选
	if studentID := c.Query("student_id"); studentID != "" {
		query = query.Where("student_id = ?", studentID)
	}

	// 科目筛选
	if courseID := c.Query("course_id"); courseID != "" {
		query = query.Where("course_id = ?", courseID)
	}

	// 状态筛选
	if status := c.Query("status"); status != "" {
		query = query.Where("status = ?", status)
	}

	// 按时间倒序
	query = query.Order("start_time DESC")

	if err := query.Find(&schedules).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, schedules)
}

// Create 创建排课
// @Summary 创建排课
// @Tags 排课管理
// @Security BearerAuth
// @Accept json
// @Produce json
// @Param schedule body models.Schedule true "排课信息"
// @Success 201 {object} models.Schedule
// @Router /schedules [post]
func (h *ScheduleHandler) Create(c *gin.Context) {
	var schedule models.Schedule
	if err := c.ShouldBindJSON(&schedule); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if err := h.DB.Create(&schedule).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	h.DB.Preload("Student").Preload("Course").First(&schedule, schedule.ID)
	c.JSON(http.StatusCreated, schedule)
}

// Update 更新排课
// @Summary 更新排课
// @Tags 排课管理
// @Security BearerAuth
// @Accept json
// @Produce json
// @Param id path int true "排课ID"
// @Param schedule body models.Schedule true "排课信息"
// @Success 200 {object} models.Schedule
// @Failure 404 {object} map[string]string
// @Router /schedules/{id} [put]
func (h *ScheduleHandler) Update(c *gin.Context) {
	id := c.Param("id")
	var schedule models.Schedule
	if err := h.DB.First(&schedule, id).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "排课不存在"})
		return
	}
	if err := c.ShouldBindJSON(&schedule); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	if err := h.DB.Save(&schedule).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	h.DB.Preload("Student").Preload("Course").First(&schedule, schedule.ID)
	c.JSON(http.StatusOK, schedule)
}

// Delete 删除排课
// @Summary 删除排课
// @Tags 排课管理
// @Security BearerAuth
// @Param id path int true "排课ID"
// @Success 200 {object} map[string]string
// @Router /schedules/{id} [delete]
func (h *ScheduleHandler) Delete(c *gin.Context) {
	id := c.Param("id")
	if err := h.DB.Delete(&models.Schedule{}, id).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "删除成功"})
}

// GetTodaySchedules 获取今日排课
// @Summary 获取今日排课
// @Tags 排课管理
// @Security BearerAuth
// @Produce json
// @Success 200 {object} map[string][]models.Schedule
// @Router /schedules/today [get]
func (h *ScheduleHandler) GetTodaySchedules(c *gin.Context) {
	var schedules []models.Schedule

	now := time.Now()
	startOfDay := time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, now.Location())
	endOfDay := startOfDay.Add(24 * time.Hour)

	err := h.DB.Preload("Student").Preload("Course").
		Where("start_time >= ? AND start_time < ?", startOfDay, endOfDay).
		Find(&schedules).Error

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"data": schedules})
}

// App 专用接口 - 返回简化的今日课程数据
type DashboardSchedule struct {
	ID          uint   `json:"id"`
	StudentName string `json:"student_name"`
	TimeSlot    string `json:"time_slot"`
	Subject     string `json:"subject"`
	Status      string `json:"status"` // pending, ongoing, completed
	Date        string `json:"date"`   // yyyy-MM-dd 格式
}

// GetDashboardToday App专用 - 获取今日课程
// @Summary 获取今日课程（App专用）
// @Tags App接口
// @Produce json
// @Success 200 {array} DashboardSchedule
// @Router /dashboard/today [get]
func (h *ScheduleHandler) GetDashboardToday(c *gin.Context) {
	var schedules []models.Schedule

	now := time.Now()
	startOfDay := time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, now.Location())
	endOfDay := startOfDay.Add(24 * time.Hour)

	err := h.DB.Preload("Student").Preload("Course").
		Where("start_time >= ? AND start_time < ?", startOfDay, endOfDay).
		Order("start_time ASC").
		Find(&schedules).Error

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	result := make([]DashboardSchedule, 0, len(schedules))
	for _, s := range schedules {
		// 转换到本地时区
		localStart := s.StartTime.In(time.Local)
		localEnd := s.EndTime.In(time.Local)

		status := "pending"
		if now.After(localEnd) {
			status = "completed"
		} else if now.After(localStart) && now.Before(localEnd) {
			status = "ongoing"
		}

		timeSlot := localStart.Format("15:04") + "-" + localEnd.Format("15:04")

		result = append(result, DashboardSchedule{
			ID:          s.ID,
			StudentName: s.Student.Name,
			TimeSlot:    timeSlot,
			Subject:     s.Course.Name,
			Status:      status,
			Date:        localStart.Format("2006-01-02"),
		})
	}

	c.JSON(http.StatusOK, result)
}

// GetDashboardByDate App专用 - 按日期查询课程
// @Summary 按日期获取课程（App专用）
// @Tags App接口
// @Produce json
// @Param date query string true "日期 (yyyy-MM-dd)"
// @Success 200 {array} DashboardSchedule
// @Failure 400 {object} map[string]string
// @Router /dashboard/date [get]
func (h *ScheduleHandler) GetDashboardByDate(c *gin.Context) {
	dateStr := c.Query("date")
	if dateStr == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "date parameter is required"})
		return
	}

	targetDate, err := time.Parse("2006-01-02", dateStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid date format, use yyyy-MM-dd"})
		return
	}

	var schedules []models.Schedule

	startOfDay := time.Date(targetDate.Year(), targetDate.Month(), targetDate.Day(), 0, 0, 0, 0, time.Local)
	endOfDay := startOfDay.Add(24 * time.Hour)

	err = h.DB.Preload("Student").Preload("Course").
		Where("start_time >= ? AND start_time < ?", startOfDay, endOfDay).
		Order("start_time ASC").
		Find(&schedules).Error

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	now := time.Now()
	result := make([]DashboardSchedule, 0, len(schedules))
	for _, s := range schedules {
		// 转换到本地时区
		localStart := s.StartTime.In(time.Local)
		localEnd := s.EndTime.In(time.Local)

		status := "pending"
		if now.After(localEnd) {
			status = "completed"
		} else if now.After(localStart) && now.Before(localEnd) {
			status = "ongoing"
		}

		timeSlot := localStart.Format("15:04") + "-" + localEnd.Format("15:04")

		result = append(result, DashboardSchedule{
			ID:          s.ID,
			StudentName: s.Student.Name,
			TimeSlot:    timeSlot,
			Subject:     s.Course.Name,
			Status:      status,
			Date:        localStart.Format("2006-01-02"),
		})
	}

	c.JSON(http.StatusOK, result)
}
