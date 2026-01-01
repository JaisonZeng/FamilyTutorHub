package main

import (
	"log"
	"time"

	"tutor-management/config"
	_ "tutor-management/docs" // swagger docs
	"tutor-management/handlers"
	"tutor-management/models"

	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	swaggerFiles "github.com/swaggo/files"
	ginSwagger "github.com/swaggo/gin-swagger"
)

// @title 家教管理系统 API
// @version 1.0
// @description 家教管理后台系统接口文档

// @host localhost:8080
// @BasePath /api

// @securityDefinitions.apikey BearerAuth
// @in header
// @name Authorization
// @description 输入 Bearer {token}

func init() {
	// 设置默认时区为上海（北京时间）
	loc, err := time.LoadLocation("Asia/Shanghai")
	if err != nil {
		log.Println("加载时区失败，使用系统默认时区:", err)
		return
	}
	time.Local = loc
}

func main() {
	// 加载配置
	cfg, err := config.LoadConfig()
	if err != nil {
		log.Fatal("加载配置失败:", err)
	}

	// 初始化数据库
	db, err := config.InitDatabase(cfg)
	if err != nil {
		log.Fatal("数据库连接失败:", err)
	}

	// 自动迁移
	db.AutoMigrate(&models.Student{}, &models.Course{}, &models.Schedule{}, &models.ExamResult{}, &models.User{})

	// 初始化 Gin
	r := gin.Default()

	// 配置 CORS
	r.Use(cors.New(cors.Config{
		AllowOrigins:     []string{"*"},
		AllowMethods:     []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowHeaders:     []string{"Origin", "Content-Type", "Authorization"},
		AllowCredentials: true,
	}))

	// 初始化 handlers
	authHandler := handlers.NewAuthHandler(db)
	studentHandler := handlers.NewStudentHandler(db)
	courseHandler := handlers.NewCourseHandler(db)
	scheduleHandler := handlers.NewScheduleHandler(db)
	examResultHandler := handlers.NewExamResultHandler(db)
	trendHandler := handlers.NewTrendHandler()

	// 初始化管理员账号
	authHandler.InitAdmin()

	api := r.Group("/api")
	{
		// 公开接口 - 无需认证
		api.POST("/login", authHandler.Login)
		api.POST("/logout", authHandler.Logout)

		// App 专用接口 - 无需认证
		api.GET("/dashboard/today", scheduleHandler.GetDashboardToday)
		api.GET("/dashboard/date", scheduleHandler.GetDashboardByDate)

		// 成绩趋势分析图表 - 无需认证（图片资源）
		api.GET("/analysis/:student_id/trend.png", trendHandler.GetTrendChart)

		// 需要认证的接口
		protected := api.Group("")
		protected.Use(handlers.JWTMiddleware())
		{
			protected.GET("/currentUser", authHandler.CurrentUser)

			protected.GET("/students", studentHandler.GetAll)
			protected.POST("/students", studentHandler.Create)
			protected.PUT("/students/:id", studentHandler.Update)
			protected.DELETE("/students/:id", studentHandler.Delete)

			protected.GET("/courses", courseHandler.GetAll)
			protected.POST("/courses", courseHandler.Create)
			protected.PUT("/courses/:id", courseHandler.Update)
			protected.DELETE("/courses/:id", courseHandler.Delete)

			protected.GET("/schedules", scheduleHandler.GetAll)
			protected.GET("/schedules/search", scheduleHandler.Search)
			protected.POST("/schedules", scheduleHandler.Create)
			protected.PUT("/schedules/:id", scheduleHandler.Update)
			protected.DELETE("/schedules/:id", scheduleHandler.Delete)
			protected.GET("/schedules/today", scheduleHandler.GetTodaySchedules)

			protected.GET("/exam-results", examResultHandler.GetAll)
			protected.POST("/exam-results", examResultHandler.Create)
			protected.PUT("/exam-results/:id", examResultHandler.Update)
			protected.DELETE("/exam-results/:id", examResultHandler.Delete)
			protected.GET("/exam-results/student/:student_id", examResultHandler.GetByStudent)
		}
	}

	// Swagger 文档
	r.GET("/swagger/*any", ginSwagger.WrapHandler(swaggerFiles.Handler))

	// 启动服务
	r.Run(":8080")
}
