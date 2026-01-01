package models

import (
	"time"
)

type Schedule struct {
	ID        uint      `json:"id" gorm:"primarykey"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
	StudentID uint      `json:"student_id"`
	Student   Student   `json:"student" gorm:"foreignKey:StudentID"`
	CourseID  uint      `json:"course_id"`
	Course    Course    `json:"course" gorm:"foreignKey:CourseID"`
	StartTime time.Time `json:"start_time"`
	EndTime   time.Time `json:"end_time"`
	Status    string    `json:"status"` // "scheduled", "completed", "cancelled"
}
