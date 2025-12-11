package cn.mapleisle.osaka.data

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HistoryManager {
    private const val HISTORY_DIR = "History"
    private val _historyUpdates = MutableSharedFlow<File>(replay = 0)
    val historyUpdates = _historyUpdates.asSharedFlow()

    fun saveHistory(context: Context, text: String): File? {
        return try {
            val historyDir = File(context.getExternalFilesDir(null), HISTORY_DIR)
            if (!historyDir.exists()) historyDir.mkdirs()
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val filename = "Log_$timestamp.txt"
            val file = File(historyDir, filename)
            file.writeText(text)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun notifyNewHistory(file: File) {
        _historyUpdates.emit(file)
    }

    fun getHistoryFiles(context: Context): List<File> {
        val historyDir = File(context.getExternalFilesDir(null), HISTORY_DIR)
        return if (historyDir.exists()) {
            historyDir.listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
}
