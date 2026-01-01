package models

import (
	"time"
)

type ExamResult struct {
	ID        uint      `json:"id" gorm:"primarykey"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
	StudentID uint      `json:"student_id"`
	Student   *Student  `json:"student,omitempty" gorm:"foreignKey:StudentID"`
	CourseID  uint      `json:"course_id"`
	Course    *Course   `json:"course,omitempty" gorm:"foreignKey:CourseID"`
	ExamType  string    `json:"exam_type"` // midterm期中, final期末, quiz小测
	ExamName  string    `json:"exam_name"` // 考试名称，如"第一次月考"
	Score     float64   `json:"score"`
	FullScore float64   `json:"full_score"` // 满分
	ExamDate  time.Time `json:"exam_date"`
	Comment   string    `json:"comment"`
}
