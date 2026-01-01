package config

import (
	"fmt"
	"log"
	"os"
	"path/filepath"

	"github.com/glebarez/sqlite"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
)

// InitDatabase 根据配置初始化数据库连接
func InitDatabase(config *Config) (*gorm.DB, error) {
	switch config.Database.Type {
	case "sqlite":
		return initSQLite(config)
	case "mysql":
		return initMySQL(config)
	default:
		return nil, fmt.Errorf("不支持的数据库类型: %s", config.Database.Type)
	}
}

func initSQLite(config *Config) (*gorm.DB, error) {
	dbPath := config.Database.SQLite
	if dbPath == "" {
		dbPath = "data/tutor.db"
	}

	// 确保目录存在
	dir := filepath.Dir(dbPath)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return nil, fmt.Errorf("创建数据目录失败: %w", err)
	}

	log.Printf("使用 SQLite 数据库: %s", dbPath)
	db, err := gorm.Open(sqlite.Open(dbPath), &gorm.Config{})
	if err != nil {
		return nil, fmt.Errorf("SQLite 连接失败: %w", err)
	}

	// 启用外键约束
	db.Exec("PRAGMA foreign_keys = ON")

	return db, nil
}

func initMySQL(config *Config) (*gorm.DB, error) {
	dsn := fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?charset=utf8mb4&parseTime=True&loc=Local",
		config.Database.User,
		config.Database.Password,
		config.Database.Host,
		config.Database.Port,
		config.Database.DBName,
	)

	log.Printf("使用 MySQL 数据库: %s:%s/%s", config.Database.Host, config.Database.Port, config.Database.DBName)
	db, err := gorm.Open(mysql.Open(dsn), &gorm.Config{})
	if err != nil {
		return nil, fmt.Errorf("MySQL 连接失败: %w", err)
	}

	return db, nil
}
