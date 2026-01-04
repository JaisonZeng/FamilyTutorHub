package com.tutor.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * 统一的UI设计规范
 * 保持整个应用的视觉一致性
 */
object AppDesign {

    // 圆角规范
    object Radius {
        val small = 8.dp
        val medium = 12.dp
        val large = 16.dp
    }

    // 卡片阴影
    object Elevation {
        val card = 4.dp
        val button = 2.dp
    }

    // 按钮高度
    object ButtonHeight {
        val large = 50.dp
        val medium = 44.dp
        val small = 36.dp
    }

    // 间距
    object Spacing {
        val tiny = 4.dp
        val small = 8.dp
        val medium = 16.dp
        val large = 24.dp
        val xlarge = 32.dp
    }
}