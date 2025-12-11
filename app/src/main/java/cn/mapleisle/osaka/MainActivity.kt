package cn.mapleisle.osaka

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.util.Consumer
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cn.mapleisle.osaka.data.ConfigManager
import cn.mapleisle.osaka.ui.HistoryScreen
import cn.mapleisle.osaka.ui.HomeScreen
import cn.mapleisle.osaka.ui.SettingsScreen
import cn.mapleisle.osaka.ui.theme.OsakaTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_AUTO_START = "cn.mapleisle.osaka.ACTION_AUTO_START"
    }

    // Launchers held at Activity level but initialized in Compose
    // Actually, in Compose, we use rememberLauncherForActivityResult, so we can keep the logic inside setContent
    // but we need to pass the triggers down.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppEntry()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    @Composable
    fun AppEntry() {
        val context = this
        val configManager = remember { ConfigManager(context) }
        var themeMode by remember { mutableIntStateOf(configManager.themeMode) }
        val navController = rememberNavController()
        var isRecording by remember { mutableStateOf(isServiceRunning(ScreenCaptureService::class.java)) }

        // Resume state check
        DisposableEffect(Unit) {
            val lifecycleObserver = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    isRecording = isServiceRunning(ScreenCaptureService::class.java)
                }
            }
            lifecycle.addObserver(lifecycleObserver)
            onDispose { lifecycle.removeObserver(lifecycleObserver) }
        }

        // --- Permissions & Recording Logic ---
        val screenCaptureLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // Config is already saved in SettingsScreen, so we just start service
                startService(result.resultCode, result.data!!)
                isRecording = true

                if (Settings.canDrawOverlays(context)) {
                    startService(Intent(context, OverlayService::class.java))
                    OverlayService.updateState("Recording")
                    OverlayService.updateProcessingStatus("正在录音中...")
                    moveTaskToBack(true) // Minimize app
                }
            }
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                if (!Settings.canDrawOverlays(context)) {
                    Toast.makeText(context, "请授予悬浮窗权限", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
                }
            } else {
                Toast.makeText(context, "缺少必要权限", Toast.LENGTH_SHORT).show()
            }
        }

        fun startRecordingFlow() {
            if (configManager.apiKey.isBlank()) {
                Toast.makeText(context, "请先设置 API Key", Toast.LENGTH_SHORT).show()
                navController.navigate("settings")
                return
            }

            val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(perms.toTypedArray())
        }

        fun stopRecordingFlow() {
            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.action = ScreenCaptureService.ACTION_STOP_COMMAND
            startService(intent)
            isRecording = false
        }

        // --- Auto Start Logic ---
        // This is triggered when OverlayService sends "ACTION_AUTO_START"
        LaunchedEffect(Unit) {
            handleAutoStart(screenCaptureLauncher)
        }

        DisposableEffect(Unit) {
            val listener = Consumer<Intent> {
                handleAutoStart(screenCaptureLauncher)
            }
            addOnNewIntentListener(listener)
            onDispose { removeOnNewIntentListener(listener) }
        }


        // --- UI ---
        OsakaTheme(themeMode = themeMode) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            isRecording = isRecording,
                            onStartRecording = { startRecordingFlow() },
                            onStopRecording = { stopRecordingFlow() },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToHistory = { navController.navigate("history") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onThemeChanged = { newMode -> themeMode = newMode }
                        )
                    }
                    composable("history") {
                        HistoryScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
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

    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
