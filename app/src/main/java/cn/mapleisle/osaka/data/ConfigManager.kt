package cn.mapleisle.osaka.data

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_config"

        // Keys
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model_name"
        private const val KEY_TIMEOUT = "timeout"
        private const val KEY_RETRY = "retry"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_THEME_MODE = "theme_mode" // 0: System, 1: Light, 2: Dark
    }

    // Theme Mode
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    // Base URL
    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "https://generativelanguage.googleapis.com/v1beta") ?: ""
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    // API Key
    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    // Model Name
    var modelName: String
        get() = prefs.getString(KEY_MODEL, "gemini-2.0-flash") ?: ""
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    // Timeout
    var timeout: Long
        get() = prefs.getString(KEY_TIMEOUT, "30")?.toLongOrNull() ?: 30L
        set(value) = prefs.edit().putString(KEY_TIMEOUT, value.toString()).apply()

    // Retry
    var retry: Int
        get() = prefs.getString(KEY_RETRY, "3")?.toIntOrNull() ?: 3
        set(value) = prefs.edit().putString(KEY_RETRY, value.toString()).apply()

    // System Prompt
    var systemPrompt: String
        get() = prefs.getString(KEY_SYSTEM_PROMPT, "You are a helpful assistant. Please transcribe the audio verbatim.") ?: ""
        set(value) = prefs.edit().putString(KEY_SYSTEM_PROMPT, value).apply()

    // Data class for read-only snapshot
    data class ConfigData(
        val baseUrl: String,
        val apiKey: String,
        val model: String,
        val timeout: Long,
        val retry: Int,
        val systemPrompt: String
    )

    fun getConfigSnapshot(): ConfigData {
        return ConfigData(baseUrl, apiKey, modelName, timeout, retry, systemPrompt)
    }
}
