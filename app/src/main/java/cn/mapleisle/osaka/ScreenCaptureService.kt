package cn.mapleisle.osaka

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenCaptureService : Service() {

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var recordingJob: Job? = null
    private var isRecording = false

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val ACTION_STOP_COMMAND = "cn.mapleisle.osaka.ACTION_STOP"
        var currentFilePath: String? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_COMMAND) {
            handleStopAndProcess()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

        if (resultCode != Activity.RESULT_OK || resultData == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceNotification()
        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData)
        startAudioCapture()

        return START_STICKY
    }

    private fun handleStopAndProcess() {
        stopRecordingResources()
        OverlayService.updateState("Processing")
        OverlayService.updateResult("录制完成，开始处理...")

        serviceScope.launch {
            val pcmPath = currentFilePath
            if (pcmPath != null) {
                processFile(pcmPath)
            } else {
                withContext(Dispatchers.Main) {
                    OverlayService.updateState("Error")
                    OverlayService.updateResult("错误: 文件路径为空")
                }
                stopSelf()
            }
        }
    }

    private suspend fun processFile(pcmPath: String) {
        try {
            val pcmFile = File(pcmPath)
            val wavPath = pcmFile.parent + "/final_audio.wav"
            val wavFile = File(wavPath)
            if (wavFile.exists()) wavFile.delete()

            withContext(Dispatchers.Main) {
                OverlayService.updateResult("正在转码 (PCM -> WAV)...")
            }

            val cmd = "-f s16le -ar 44100 -ac 2 -i \"$pcmPath\" \"$wavPath\""
            val session = FFmpegKit.execute(cmd)

            if (ReturnCode.isSuccess(session.returnCode)) {
                if (pcmFile.exists()) pcmFile.delete()
                uploadToApi(wavFile)
            } else {
                withContext(Dispatchers.Main) {
                    OverlayService.updateState("Error")
                    OverlayService.updateResult("转码失败: ${session.failStackTrace}")
                }
                stopSelf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private suspend fun uploadToApi(audioFile: File) {
        val config = getConfig()

        // 自动修正 OpenAI URL 为 Gemini Native URL (防止用户填错)
        val effectiveBaseUrl = if (config.baseUrl.contains("openai")) {
            "https://generativelanguage.googleapis.com/v1beta/"
        } else {
            config.baseUrl
        }

        withContext(Dispatchers.Main) {
            OverlayService.updateResult("正在上传到 ${config.model} ...")
        }

        try {
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            // ✅ 核心逻辑修正：System Prompt 拼接
            // Gemini 的 contents 结构不支持 role: system。
            // 我们把 System Prompt 加在文本提示词的前面，效果是一样的。
            val finalPrompt = if (config.systemPrompt.isNotBlank()) {
                "${config.systemPrompt}\n\n[Instruction]: Please listen to this audio carefully and provide a verbatim transcription."
            } else {
                "Please listen to this audio carefully and provide a verbatim transcription."
            }

            // ✅ 构造 Gemini 原生请求
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            // 1. 组合后的文本提示
                            GeminiPart(text = finalPrompt),
                            // 2. 音频数据
                            GeminiPart(
                                inline_data = GeminiBlob(
                                    mime_type = "audio/wav",
                                    data = base64Audio
                                )
                            )
                        )
                    )
                )
            )

            val apiService = NetworkClient.createService(effectiveBaseUrl, config.timeout, config.retry)

            // 去掉 Bearer 前缀，Gemini 原生只需要 Key
            val cleanKey = config.apiKey.replace("Bearer ", "").trim()

            val response = apiService.generateContent(
                model = config.model,
                apiKey = cleanKey,
                request = request
            )

            val candidate = response.candidates?.firstOrNull()
            val resultText = candidate?.content?.parts
                ?.mapNotNull { it.text } // 过滤掉没有 text 的 part
                ?.joinToString("\n")     // 用换行符拼接
                ?: "API 返回为空 (可能被拦截)"

            // ✅ 保存历史记录
            saveToHistory(resultText)

            withContext(Dispatchers.Main) {
                OverlayService.updateState("Done")
                OverlayService.updateResult(resultText)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                OverlayService.updateState("Error")
                val errorMsg = when {
                    e.message?.contains("404") == true -> "404 错误: 路径不对 (请检查 BaseURL 是否为 Google 原生)"
                    e.message?.contains("400") == true -> "400 错误: 参数错误 (可能是模型名填错)"
                    else -> "API 错误: ${e.message}"
                }
                OverlayService.updateResult(errorMsg)
            }
        } finally {
            stopSelf()
        }
    }

    private fun saveToHistory(text: String) {
        try {
            val historyDir = File(getExternalFilesDir(null), "History")
            if (!historyDir.exists()) historyDir.mkdirs()
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val filename = "Log_$timestamp.txt"
            val file = File(historyDir, filename)
            file.writeText(text)
            Log.d("History", "Saved to ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startAudioCapture() {
        // ... (录音初始化逻辑，保持不变) ...
        // 为了节省篇幅，这里省略重复代码。
        // 请保留你原来文件中 startAudioCapture, startWritingToFile 等底层录音代码
        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (dir != null && !dir.exists()) dir.mkdirs()
        val fileName = "REC_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pcm"
        val file = File(dir, fileName)
        currentFilePath = file.absolutePath

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()

            try {
                audioRecord = AudioRecord.Builder()
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT) * 2)
                    .setAudioPlaybackCaptureConfig(config)
                    .build()

                audioRecord?.startRecording()
                isRecording = true
                startWritingToFile(file)

            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        } else {
            stopSelf()
        }
    }

    // ... startWritingToFile, stopRecordingResources, Notifications 等辅助方法保持不变 ...
    private fun startWritingToFile(file: File) {
        recordingJob = serviceScope.launch {
            var outputStream: FileOutputStream? = null
            try {
                outputStream = FileOutputStream(file)
                val buffer = ByteArray(1024 * 4)
                while (isActive && isRecording) {
                    val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readResult > 0) {
                        outputStream.write(buffer, 0, readResult)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { outputStream?.close() } catch (e: Exception) {}
            }
        }
    }

    private fun stopRecordingResources() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) { e.printStackTrace() }
        audioRecord = null
        mediaProjection?.stop()
        mediaProjection = null
        recordingJob?.cancel()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "screen_capture_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("系统内录中")
            .setContentText("正在捕获应用音频...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notificationBuilder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("screen_capture_channel", "Screen Capture", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    private fun getConfig(): ConfigData {
        val prefs = getSharedPreferences("app_config", Context.MODE_PRIVATE)
        return ConfigData(
            prefs.getString("base_url", "https://generativelanguage.googleapis.com/v1beta/openai/") ?: "",
            prefs.getString("api_key", "") ?: "",
            prefs.getString("model_name", "gemini-2.0-flash") ?: "",
            prefs.getString("timeout", "30")?.toLongOrNull() ?: 30L,
            prefs.getString("retry", "3")?.toIntOrNull() ?: 3,
            prefs.getString("system_prompt", "") ?: ""
        )
    }

    data class ConfigData(val baseUrl: String, val apiKey: String, val model: String, val timeout: Long, val retry: Int, val systemPrompt: String)

    override fun onDestroy() {
        super.onDestroy()
        stopRecordingResources()
        serviceScope.cancel()
    }
}