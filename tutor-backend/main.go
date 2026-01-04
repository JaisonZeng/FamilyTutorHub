package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"tutor-management/config"
	_ "tutor-management/docs" // swagger docs
	"tutor-management/handlers"
	"tutor-management/middleware"
	"tutor-management/models"
	"tutor-management/utils"

	"github.com/gin-gonic/gin"
	swaggerFiles "github.com/swaggo/files"
	ginSwagger "github.com/swaggo/gin-swagger"
	"go.uber.org/zap"
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
	// 初始化日志系统
	logPath := os.Getenv("LOG_PATH")
	if logPath == "" {
		logPath = "./logs/app.log"
	}
	logLevel := os.Getenv("LOG_LEVEL")
	if logLevel == "" {
		logLevel = "info"
	}
	isDev := os.Getenv("ENV") != "production"

	if err := utils.InitLogger(logPath, logLevel, isDev); err != nil {
		log.Fatal("初始化日志系统失败:", err)
	}
	defer utils.Sync()

	utils.Info("Starting tutor management system",
		zap.String("env", os.Getenv("ENV")),
		zap.String("log_level", logLevel),
	)

	// 加载配置
	cfg, err := config.LoadConfig()
	if err != nil {
		utils.Fatal("加载配置失败", zap.Error(err))
	}

	// 初始化数据库
	db, err := config.InitDatabase(cfg)
	if err != nil {
		utils.Fatal("数据库连接失败", zap.Error(err))
	}

	utils.Info("Database connected successfully")

	// 自动迁移
	db.AutoMigrate(&models.Student{}, &models.Course{}, &models.Schedule{}, &models.ExamResult{}, &models.User{})

	// 初始化安全中间件
	middleware.InitBlacklist()
	middleware.InitRateLimiters()

	// 设置Gin模式
	if !isDev {
		gin.SetMode(gin.ReleaseMode)
	}

	// 初始化 Gin (不使用默认中间件)
	r := gin.New()

	// 添加安全中间件
	r.Use(middleware.Recovery())
	r.Use(middleware.SecurityMiddleware())
	r.Use(middleware.BlacklistMiddleware())
	r.Use(middleware.RateLimitMiddleware())
	r.Use(middleware.RequestLogger())
	r.Use(middleware.SlowRequestLogger(2 * time.Second))

	// 配置 CORS (生产环境应该限制来源)
	allowedOrigins := []string{"*"}
	if os.Getenv("ENV") == "production" {
		// 生产环境应该设置具体的域名
		// allowedOrigins = []string{"https://your-domain.com"}
	}
	r.Use(middleware.CORSMiddleware(allowedOrigins))

	// 初始化 handlers
	authHandler := handlers.NewAuthHandler(db)
	studentHandler := handlers.NewStudentHandler(db)
	courseHandler := handlers.NewCourseHandler(db)
	scheduleHandler := handlers.NewScheduleHandler(db)
	examResultHandler := handlers.NewExamResultHandler(db)
	trendHandler := handlers.NewTrendHandler()
	healthHandler := handlers.NewHealthHandler(db)

	// 初始化管理员账号
	authHandler.InitAdmin()

	// 健康检查路由
	r.GET("/health", healthHandler.HealthCheck)
	r.GET("/health/ready", healthHandler.ReadinessCheck)
	r.GET("/metrics", healthHandler.MetricsCheck)

	api := r.Group("/api")
	// API 路由使用更严格的限流
	api.Use(middleware.APIRateLimitMiddleware())
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
			protected.PUT("/password", authHandler.ChangePassword)

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

	// 创建HTTP服务器
	srv := &http.Server{
		Addr:    ":8080",
		Handler: r,
	}

	// 启动服务器
	go func() {
		utils.Info("Server starting on :8080")
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			utils.Fatal("Server failed to start", zap.Error(err))
		}
	}()

	// 优雅关闭
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	utils.Info("Shutting down server...")

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		utils.Error("Server forced to shutdown", zap.Error(err))
	}

	utils.Info("Server exited")
}
