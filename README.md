# Osaka Recorder (大阪录音姬) 🎙️

一个基于 Android 的悬浮窗实时录音与 AI 转写工具。支持系统内录，并使用 Gemini/OpenAI 模型进行实时转写。

## ✨ 功能特点
- **系统内录**：通过 MediaProjection 录制设备内部声音（无杂音）。
- **悬浮窗控制**：全局悬浮窗，随时开始/停止录制。
- **AI 转写**：支持自定义 API Endpoint（Gemini/OpenAI 格式）。
- **历史记录**：自动保存识别结果到本地。
- **自定义角色**：支持设置 System Prompt（如“会议纪要”、“日语翻译”）。

## 🛠️ 技术栈
- **语言**：Kotlin
- **UI**：Jetpack Compose
- **音频处理**：FFmpeg-Kit (PCM -> WAV)
- **网络**：Retrofit + OkHttp
- **架构**：Foreground Service (前台服务) + ViewModel

## 🚀 如何使用
1. 下载并安装 APK。
2. 授予录音、悬浮窗、通知权限。
3. 在设置中填入你的 API Key 和 Base URL。
    - 推荐模型：`gemini-2.5-flash`
4. 点击“启动悬浮窗”。

## ⚠️ 注意事项
- 本项目仅供学习交流。
- 请确保你使用的 API Key 有足够的额度。

## 📄 License
MIT License