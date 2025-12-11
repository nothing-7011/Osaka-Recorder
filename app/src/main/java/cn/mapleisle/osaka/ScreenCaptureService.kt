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
import cn.mapleisle.osaka.data.ConfigManager
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
        OverlayService.updateState("Ready")
        OverlayService.updateResult("Recording saved, processing in background...")

        val intent = Intent(this, ProcessingService::class.java).apply {
            action = ProcessingService.ACTION_ADD_TASK
            putExtra(ProcessingService.EXTRA_PCM_PATH, currentFilePath)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        stopSelf()
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

    override fun onDestroy() {
        super.onDestroy()
        stopRecordingResources()
        serviceScope.cancel()
    }
}