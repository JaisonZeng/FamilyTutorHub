package models

import "time"

type Student struct {
	ID          uint      `json:"id" gorm:"primarykey"`
	CreatedAt   time.Time `json:"created_at"`
	UpdatedAt   time.Time `json:"updated_at"`
	Name        string    `json:"name"`
	ParentPhone string    `json:"parent_phone"`
	Grade       string    `json:"grade"` // e.g., "初二"
	Notes       string    `json:"notes"` // 备注
}
