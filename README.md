# Osaka Recorder

一个基于 Android 的悬浮窗实时录音与 AI 转写工具。支持系统内录，并使用 Gemini/OpenAI 模型进行实时转写。

## ✨ 功能特点
- **系统内录**：通过 MediaProjection 录制设备内部声音（无杂音）。
- **悬浮窗控制**：全局悬浮窗，随时开始/停止录制，支持查看历史记录。
- **连续录制**：后台处理转码与上传，允许立刻开始下一段录制。
- **AI 转写**：支持自定义 API Endpoint（Gemini/OpenAI 格式）。
- **Markdown 支持**：悬浮窗与历史记录支持 Markdown 格式渲染。
- **主题切换**：支持跟随系统、浅色模式和深色模式。
- **自定义角色**：支持设置 System Prompt（如“会议纪要”、“日语翻译”）。

## 🛠️ 技术栈
- **语言**：Kotlin
- **UI**：Jetpack Compose (Material3) + Navigation Compose
- **音频处理**：FFmpeg-Kit (PCM -> WAV)
- **网络**：Retrofit + OkHttp
- **架构**：
    - `ScreenCaptureService`: 负责前台录音
    - `ProcessingService`: 负责后台转码与上传 (Foreground Service)
    - `OverlayService`: 悬浮窗 UI
- **其他**：Markwon (Markdown 渲染)

## 🚀 如何使用
1. 下载并安装 APK。
2. 授予录音、悬浮窗、通知权限。
3. 在设置页面填入你的 API Key 和 Base URL。
    - 推荐模型：`gemini-2.0-flash`
4. 点击“Start Recording”或悬浮窗按钮开始录制。

## ⚠️ 注意事项
- 本项目仅供学习交流。
- 请确保你使用的 API Key 有足够的额度。

## 📄 License
MIT License
