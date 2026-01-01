package models

import "time"

type User struct {
	ID        uint      `json:"id" gorm:"primarykey"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
	Username  string    `json:"username" gorm:"unique;not null"`
	Password  string    `json:"-"` // 不返回密码
	Name      string    `json:"name"`
	Avatar    string    `json:"avatar"`
}
