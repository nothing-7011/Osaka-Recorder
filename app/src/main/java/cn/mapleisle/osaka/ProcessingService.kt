package cn.mapleisle.osaka

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Base64
import androidx.core.app.NotificationCompat
import cn.mapleisle.osaka.data.ConfigManager
import cn.mapleisle.osaka.data.HistoryManager
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

class ProcessingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val taskQueue = ConcurrentLinkedQueue<String>()
    private var isProcessing = false

    companion object {
        const val EXTRA_PCM_PATH = "pcm_path"
        const val ACTION_ADD_TASK = "cn.mapleisle.osaka.ACTION_ADD_TASK"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ADD_TASK) {
            val pcmPath = intent.getStringExtra(EXTRA_PCM_PATH)
            if (pcmPath != null) {
                taskQueue.add(pcmPath)
                processQueue()
            }
        }
        startForegroundServiceNotification()
        return START_STICKY
    }

    private fun processQueue() {
        if (isProcessing) return
        if (taskQueue.isEmpty()) {
            stopSelf()
            return
        }

        isProcessing = true
        val pcmPath = taskQueue.poll()

        if (pcmPath != null) {
            serviceScope.launch {
                try {
                    processFile(pcmPath)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isProcessing = false
                    processQueue()
                }
            }
        } else {
            isProcessing = false
        }
    }

    private suspend fun processFile(pcmPath: String) {
        val pcmFile = File(pcmPath)
        val wavPath = pcmFile.parent + "/final_audio.wav"
        val wavFile = File(wavPath)

        // Notify start
        withContext(Dispatchers.Main) {
            OverlayService.updateProcessingStatus("Transcoding...")
        }

        if (wavFile.exists()) wavFile.delete()

        val cmd = "-f s16le -ar 44100 -ac 2 -i \"$pcmPath\" \"$wavPath\""
        val session = FFmpegKit.execute(cmd)

        if (ReturnCode.isSuccess(session.returnCode)) {
            if (pcmFile.exists()) pcmFile.delete()
            uploadToApi(wavFile)
        } else {
             withContext(Dispatchers.Main) {
                 OverlayService.updateProcessingStatus("Transcode Error")
             }
        }
    }

    private suspend fun uploadToApi(audioFile: File) {
        val config = ConfigManager(this).getConfigSnapshot()
        val effectiveBaseUrl = if (config.baseUrl.contains("openai")) {
            "https://generativelanguage.googleapis.com/v1beta/"
        } else {
            config.baseUrl
        }

        withContext(Dispatchers.Main) {
            OverlayService.updateProcessingStatus("Uploading...")
        }

        try {
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            val finalPrompt = if (config.systemPrompt.isNotBlank()) {
                "${config.systemPrompt}\n\n[Instruction]: Please listen to this audio carefully and provide a verbatim transcription."
            } else {
                "Please listen to this audio carefully and provide a verbatim transcription."
            }

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(text = finalPrompt),
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
            val cleanKey = config.apiKey.replace("Bearer ", "").trim()

            val response = apiService.generateContent(
                model = config.model,
                apiKey = cleanKey,
                request = request
            )

            val candidate = response.candidates?.firstOrNull()
            val resultText = candidate?.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("\n")
                ?: "API Response Empty"

            val file = HistoryManager.saveHistory(this, resultText)
            if (file != null) {
                HistoryManager.notifyNewHistory(file)
                withContext(Dispatchers.Main) {
                    OverlayService.updateProcessingStatus("Done")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                OverlayService.updateProcessingStatus("Error: ${e.message}")
            }
        } finally {
            if (audioFile.exists()) audioFile.delete()
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "processing_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Processing Audio")
            .setContentText("Transcoding and Uploading...")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(2, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(2, notificationBuilder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("processing_channel", "Audio Processing", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
