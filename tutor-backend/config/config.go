package config

import (
	"fmt"
	"os"
	"path/filepath"

	"gopkg.in/yaml.v3"
)

type DatabaseConfig struct {
	Type     string `yaml:"type"`     // mysql 或 sqlite
	Host     string `yaml:"host"`     // MySQL 主机
	Port     string `yaml:"port"`     // MySQL 端口
	User     string `yaml:"user"`     // MySQL 用户名
	Password string `yaml:"password"` // MySQL 密码
	DBName   string `yaml:"dbname"`   // 数据库名称
	SQLite   string `yaml:"sqlite"`   // SQLite 文件路径
}

type Config struct {
	Database DatabaseConfig `yaml:"database"`
}

// LoadConfig 加载配置文件
func LoadConfig() (*Config, error) {
	// 默认配置
	config := &Config{
		Database: DatabaseConfig{
			Type:     "mysql",
			Host:     "localhost",
			Port:     "3306",
			User:     "tutor",
			Password: "tutor123",
			DBName:   "tutor",
			SQLite:   "data/tutor.db",
		},
	}

	// 尝试从配置文件加载
	configPath := getConfigPath()
	if data, err := os.ReadFile(configPath); err == nil {
		if err := yaml.Unmarshal(data, config); err != nil {
			return nil, fmt.Errorf("解析配置文件失败: %w", err)
		}
	}

	// 环境变量覆盖配置文件
	if dbType := os.Getenv("DB_TYPE"); dbType != "" {
		config.Database.Type = dbType
	}
	if host := os.Getenv("DB_HOST"); host != "" {
		config.Database.Host = host
	}
	if port := os.Getenv("DB_PORT"); port != "" {
		config.Database.Port = port
	}
	if user := os.Getenv("DB_USER"); user != "" {
		config.Database.User = user
	}
	if password := os.Getenv("DB_PASSWORD"); password != "" {
		config.Database.Password = password
	}
	if dbname := os.Getenv("DB_NAME"); dbname != "" {
		config.Database.DBName = dbname
	}
	if sqlite := os.Getenv("DB_SQLITE"); sqlite != "" {
		config.Database.SQLite = sqlite
	}

	return config, nil
}

func getConfigPath() string {
	// 优先使用环境变量指定的配置文件
	if path := os.Getenv("CONFIG_PATH"); path != "" {
		return path
	}
	// 默认配置文件路径
	return filepath.Join("config", "config.yaml")
}
