package cn.mapleisle.osaka

import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
// 使用扩展函数
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import android.widget.TextView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import cn.mapleisle.osaka.data.HistoryManager
import io.noties.markwon.Markwon
import kotlinx.coroutines.delay
import java.io.File

class OverlayService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: ComposeView

    companion object {
        var appState = mutableStateOf("Ready")
        var processingStatus = mutableStateOf("")
        // Removing resultText as we will rely on history

        // This keeps track of the file currently being shown
        // If null, we might show a default message
        var currentDisplayedFile = mutableStateOf<File?>(null)

        // Notification dot
        var hasNewResult = mutableStateOf(false)

        fun updateState(state: String) { appState.value = state }
        fun updateProcessingStatus(status: String) { processingStatus.value = status }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                OverlayUI()
            }
        }

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 50
        params.y = 200

        windowManager.addView(overlayView, params)
    }

    @Composable
    fun OverlayUI() {
        val haptic = LocalHapticFeedback.current
        val scrollState = rememberScrollState()
        val historyFiles = remember { mutableStateListOf<File>() }
        var showHistoryDropdown by remember { mutableStateOf(false) }

        // Load initial history
        LaunchedEffect(Unit) {
            val files = HistoryManager.getHistoryFiles(this@OverlayService)
            historyFiles.addAll(files)
            if (files.isNotEmpty() && currentDisplayedFile.value == null) {
                currentDisplayedFile.value = files.first()
            }
        }

        // Listen for new history
        LaunchedEffect(Unit) {
            HistoryManager.historyUpdates.collect { newFile ->
                historyFiles.add(0, newFile)
                hasNewResult.value = true
                // Auto-show new result if not recording? Or let user click?
                // User requirement: "hint user with a small dot"
            }
        }

        // Load content
        var displayContent by remember { mutableStateOf("Ready to Record") }
        LaunchedEffect(currentDisplayedFile.value) {
            currentDisplayedFile.value?.let {
                displayContent = it.readText()
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEE222222)), // Slightly more opaque
            modifier = Modifier
                .width(320.dp) // Wider for markdown
                .wrapContentHeight()
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val params = overlayView.layoutParams as WindowManager.LayoutParams
                        params.x += dragAmount.x.toInt()
                        params.y += dragAmount.y.toInt()
                        windowManager.updateViewLayout(overlayView, params)
                    }
                }
        ) {
            Column(modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .align(Alignment.CenterHorizontally)
                        .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Top Bar: Status + Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val statusColor = when {
                        appState.value == "Recording" -> Color.Red
                        processingStatus.value.isNotEmpty() && processingStatus.value != "Done" -> Color.Yellow
                        else -> Color.Green
                    }
                    Box(modifier = Modifier.size(10.dp).background(statusColor, RoundedCornerShape(50)))
                    Spacer(modifier = Modifier.width(8.dp))

                    val statusText = if (appState.value == "Recording") "Recording"
                                     else if (processingStatus.value.isNotEmpty() && processingStatus.value != "Done") processingStatus.value
                                     else "Ready"

                    Text(text = statusText, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))

                    IconButton(onClick = { stopSelf() }) {
                        Icon(Icons.Default.Close, "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Control Bar: History Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = {
                                showHistoryDropdown = true
                                hasNewResult.value = false // Clear dot on open
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                             Text(
                                text = currentDisplayedFile.value?.name ?: "Select History",
                                maxLines = 1,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (hasNewResult.value) {
                                Icon(Icons.Default.Notifications, contentDescription = "New", tint = Color.Red, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = showHistoryDropdown,
                            onDismissRequest = { showHistoryDropdown = false },
                            modifier = Modifier.background(Color(0xFF333333)).heightIn(max = 200.dp)
                        ) {
                            historyFiles.forEach { file ->
                                DropdownMenuItem(
                                    text = { Text(file.name, color = Color.White, fontSize = 12.sp) },
                                    onClick = {
                                        currentDisplayedFile.value = file
                                        showHistoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content Area: Markdown
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(scrollState)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                TextView(ctx).apply {
                                    setTextColor(android.graphics.Color.WHITE)
                                    textSize = 13f
                                }
                            },
                            update = { textView ->
                                val markwon = Markwon.create(textView.context)
                                markwon.setMarkdown(textView, displayContent)
                            }
                        )
                    }

                    // Palette UX: Copy button
                    val clipboardManager = LocalClipboardManager.current
                    var isCopied by remember { mutableStateOf(false) }

                    if (isCopied) {
                        LaunchedEffect(Unit) {
                            delay(2000)
                            isCopied = false
                        }
                    }

                    if (displayContent.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(displayContent))
                                isCopied = true
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color(0x33000000), RoundedCornerShape(50)) // Pill/Circle shape
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Filled.Done else Icons.Filled.ContentCopy,
                                contentDescription = if (isCopied) "Copied" else "Copy Text",
                                tint = if (isCopied) Color.Green else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (appState.value == "Recording") {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val intent = Intent(this@OverlayService, ScreenCaptureService::class.java)
                                intent.action = ScreenCaptureService.ACTION_STOP_COMMAND
                                startService(intent)
                                updateState("Stopping...")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("停止录音")
                        }
                    } else {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val intent = Intent(this@OverlayService, MainActivity::class.java)
                                // ✅ 核心修改：加上暗号
                                intent.action = MainActivity.ACTION_AUTO_START
                                // 必须加这个 Flag 才能从 Service 启动 Activity
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                // 如果 Activity 已经在后台，直接复用它，不要新建
                                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("新录音")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    private class MyLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()
        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
        override val viewModelStore: ViewModelStore get() = store
        fun performRestore(savedState: Bundle?) { savedStateRegistryController.performRestore(savedState) }
        fun handleLifecycleEvent(event: Lifecycle.Event) { lifecycleRegistry.handleLifecycleEvent(event) }
    }
}
