package handlers

import (
	"log"
	"net/http"
	"os"
	"path/filepath"

	"github.com/gin-gonic/gin"
	"github.com/vicanso/go-charts/v2"
)

func init() {
	// 加载中文字体
	fontPath := filepath.Join("fonts", "NotoSansSC-Regular.ttf")
	fontData, err := os.ReadFile(fontPath)
	if err != nil {
		log.Printf("警告: 无法加载字体文件 %s: %v", fontPath, err)
		return
	}

	err = charts.InstallFont("noto", fontData)
	if err != nil {
		log.Printf("警告: 安装字体失败: %v", err)
		return
	}

	// 获取字体并设置为默认
	font, err := charts.GetFont("noto")
	if err != nil {
		log.Printf("警告: 获取字体失败: %v", err)
		return
	}
	charts.SetDefaultFont(font)
	log.Println("中文字体加载成功")
}

// TrendHandler 成绩趋势分析处理器
type TrendHandler struct{}

// NewTrendHandler 创建趋势分析处理器
func NewTrendHandler() *TrendHandler {
	return &TrendHandler{}
}

// GetTrendChart 生成成绩变化折线图
// GET /api/analysis/:student_id/trend.png?subject=math
func (h *TrendHandler) GetTrendChart(c *gin.Context) {
	// studentID := c.Param("student_id") // 可用于从数据库查询真实数据
	subject := c.Query("subject")

	// X 轴 - 考试名称
	xAxisData := []string{"摸底考", "第一次月考", "期中考", "第三次月考", "期末模拟", "期末考"}

	// 构建数据
	var values [][]float64
	var legendLabels []string

	if subject != "" {
		// 指定科目时，只展示该科目的数据
		switch subject {
		case "math":
			legendLabels = []string{"数学"}
			values = [][]float64{{65, 72, 70, 85, 88, 92}}
		case "english":
			legendLabels = []string{"英语"}
			values = [][]float64{{80, 82, 85, 88, 90, 91}}
		default:
			legendLabels = []string{"数学"}
			values = [][]float64{{65, 72, 70, 85, 88, 92}}
		}
	} else {
		// 未指定科目时，展示数学和英语两条线对比
		legendLabels = []string{"数学", "英语"}
		values = [][]float64{
			{65, 72, 70, 85, 88, 92}, // 数学
			{80, 82, 85, 88, 90, 91}, // 英语
		}
	}

	// 创建折线图
	p, err := charts.LineRender(
		values,
		charts.TitleTextOptionFunc("阶段成绩进步趋势"),
		charts.ThemeOptionFunc(charts.ThemeLight),
		charts.XAxisDataOptionFunc(xAxisData),
		charts.LegendOptionFunc(charts.LegendOption{
			Show: charts.TrueFlag(),
			Data: legendLabels,
		}),
		// 设置图表尺寸
		charts.WidthOptionFunc(800),
		charts.HeightOptionFunc(500),
		// 设置背景色为白色
		charts.BackgroundColorOptionFunc(charts.Color{R: 255, G: 255, B: 255, A: 255}),
		// Y 轴配置
		charts.YAxisOptionFunc(charts.YAxisOption{
			Min: floatPtr(0),
			Max: floatPtr(100),
		}),
		// 设置内边距
		charts.PaddingOptionFunc(charts.Box{
			Top:    50,
			Right:  40,
			Bottom: 30,
			Left:   50,
		}),
		// 自定义渲染选项
		func(opt *charts.ChartOption) {
			// 设置平滑曲线和显示标签
			for i := range opt.SeriesList {
				opt.SeriesList[i].Label.Show = true
				opt.SeriesList[i].Label.FontSize = 11

				// 添加标记线 - 及格线和优秀线
				opt.SeriesList[i].MarkLine = charts.NewMarkLine(
					charts.SeriesMarkDataTypeAverage,
				)
			}
		},
	)

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "生成图表失败: " + err.Error()})
		return
	}

	// 渲染为 PNG
	buf, err := p.Bytes()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "渲染图片失败: " + err.Error()})
		return
	}

	// 返回 PNG 图片
	c.Header("Content-Type", "image/png")
	c.Header("Cache-Control", "max-age=3600")
	c.Data(http.StatusOK, "image/png", buf)
}

// floatPtr 返回 float64 指针
func floatPtr(v float64) *float64 {
	return &v
}
