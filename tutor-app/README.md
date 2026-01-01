# 家教课表 App

给妈妈看今日课程安排的 Android App。

## 功能

- 显示今日课程列表
- 正在上课的课程高亮显示（绿色）
- 已完成的课程显示为灰色
- 支持下拉刷新

## 配置

使用前需要修改服务器地址：

打开 `app/src/main/java/com/tutor/app/network/RetrofitClient.kt`，修改 `BASE_URL` 为你电脑的 IP 地址：

```kotlin
private const val BASE_URL = "http://你的电脑IP:8080/"
```

## 构建

1. 用 Android Studio 打开 `tutor-app` 目录
2. 等待 Gradle 同步完成
3. 连接手机或启动模拟器
4. 点击 Run 运行

## 技术栈

- Kotlin
- Jetpack Compose
- Retrofit
- Material 3
