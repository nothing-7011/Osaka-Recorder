package cn.mapleisle.osaka

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "app_config"
    private val KEY_BASE_URL = "base_url"
    private val KEY_API_KEY = "api_key"
    private val KEY_MODEL = "model_name"
    private val KEY_TIMEOUT = "timeout"
    private val KEY_RETRY = "retry"
    private val KEY_SYSTEM_PROMPT = "system_prompt"

    companion object {
        const val ACTION_AUTO_START = "cn.mapleisle.osaka.ACTION_AUTO_START"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppUI()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    @Composable
    fun AppUI() {
        // 1. 定义状态变量
        var isRecording by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()

        var baseUrl by remember { mutableStateOf("https://generativelanguage.googleapis.com/v1beta/openai/") }
        var apiKey by remember { mutableStateOf("") }
        var modelName by remember { mutableStateOf("gemini-2.0-flash") }
        var timeout by remember { mutableStateOf("30") }
        var retry by remember { mutableStateOf("3") }
        var systemPrompt by remember { mutableStateOf("You are a helpful assistant. Please transcribe the audio verbatim.") }

        var showHistoryDialog by remember { mutableStateOf(false) }

        // ==========================================
        // 2. 核心修复：必须先把 Launcher 定义在最前面！
        // ==========================================

        // 定义录屏 Launcher
        val screenCaptureLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // 保存配置
                saveConfig(baseUrl, apiKey, modelName, timeout, retry, systemPrompt)

                // 启动 Service
                startService(result.resultCode, result.data!!)
                isRecording = true

                // 启动悬浮窗并最小化
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                    OverlayService.updateState("Recording")
                    OverlayService.updateResult("正在录音中...")
                    moveTaskToBack(true)
                }
            }
        }

        // 定义权限 Launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "请授予悬浮窗权限", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
                }
            } else {
                Toast.makeText(this@MainActivity, "缺少必要权限", Toast.LENGTH_SHORT).show()
            }
        }

        // ==========================================
        // 3. 现在可以使用 screenCaptureLauncher 了
        // ==========================================

        // 加载配置 & 处理自动启动
        LaunchedEffect(Unit) {
            val config = getConfig()
            baseUrl = config.baseUrl
            apiKey = config.apiKey
            modelName = config.model
            timeout = config.timeout.toString()
            retry = config.retry.toString()
            systemPrompt = config.systemPrompt

            // 这里调用它就不会报错了，因为上面已经定义了
            handleAutoStart(screenCaptureLauncher)
        }

        // 监听 onNewIntent 的自动启动
        DisposableEffect(Unit) {
            val listener = androidx.core.util.Consumer<Intent> {
                handleAutoStart(screenCaptureLauncher)
            }
            addOnNewIntentListener(listener)
            onDispose { removeOnNewIntentListener(listener) }
        }

        // ==========================================
        // 4. UI 布局
        // ==========================================

        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = baseUrl, onValueChange = { baseUrl = it },
                        label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey, onValueChange = { apiKey = it },
                        label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelName, onValueChange = { modelName = it },
                        label = { Text("Model Name") }, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = systemPrompt, onValueChange = { systemPrompt = it },
                        label = { Text("System Prompt (角色设定)") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = timeout, onValueChange = { if (it.all { c -> c.isDigit() }) timeout = it },
                            label = { Text("超时(s)") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = retry, onValueChange = { if (it.all { c -> c.isDigit() }) retry = it },
                            label = { Text("重试") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (baseUrl.isBlank() || apiKey.isBlank()) return@Button

                            val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    ) {
                        Text("启动悬浮窗 & 录制")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("查看识别历史")
                    }
                }

                if (showHistoryDialog) {
                    HistoryDialog(onDismiss = { showHistoryDialog = false })
                }
            }
        }
    }

    // --- 历史记录组件 ---
    @Composable
    fun HistoryDialog(onDismiss: () -> Unit) {
        val historyDir = File(getExternalFilesDir(null), "History")
        val files = remember {
            if (historyDir.exists()) {
                historyDir.listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        }
        var selectedFileContent by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier.fillMaxWidth().height(600.dp).padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (selectedFileContent == null) "识别历史" else "记录详情",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (selectedFileContent == null) {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(files) { file ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        .clickable { selectedFileContent = file.readText() },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp)) {
                                        Column {
                                            Text(text = file.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(text = "${file.length() / 1024} KB", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                                Text(text = selectedFileContent ?: "")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        if (selectedFileContent != null) {
                            Button(onClick = { selectedFileContent = null }) { Text("返回列表") }
                        } else {
                            Button(onClick = onDismiss) { Text("关闭") }
                        }
                    }
                }
            }
        }
    }

    private fun handleAutoStart(launcher: ActivityResultLauncher<Intent>) {
        if (intent.action == ACTION_AUTO_START) {
            intent.action = ""
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            launcher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    private fun startService(resultCode: Int, data: Intent) {
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun saveConfig(url: String, key: String, model: String, timeout: String, retry: String, systemPrompt: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_BASE_URL, url)
            .putString(KEY_API_KEY, key)
            .putString(KEY_MODEL, model)
            .putString(KEY_TIMEOUT, timeout)
            .putString(KEY_RETRY, retry)
            .putString(KEY_SYSTEM_PROMPT, systemPrompt)
            .apply()
    }

    private fun getConfig(): ConfigData {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ConfigData(
            prefs.getString(KEY_BASE_URL, "https://generativelanguage.googleapis.com/v1beta/openai/") ?: "",
            prefs.getString(KEY_API_KEY, "") ?: "",
            prefs.getString(KEY_MODEL, "gemini-2.0-flash") ?: "",
            prefs.getString(KEY_TIMEOUT, "30")?.toLongOrNull() ?: 30L,
            prefs.getString(KEY_RETRY, "3")?.toIntOrNull() ?: 3,
            prefs.getString(KEY_SYSTEM_PROMPT, "You are a helpful assistant. Please transcribe the audio verbatim.") ?: ""
        )
    }

    data class ConfigData(val baseUrl: String, val apiKey: String, val model: String, val timeout: Long, val retry: Int, val systemPrompt: String)
}