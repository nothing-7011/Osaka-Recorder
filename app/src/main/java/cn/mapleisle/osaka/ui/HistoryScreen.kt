package cn.mapleisle.osaka.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for file list
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load files asynchronously
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val historyDir = File(context.getExternalFilesDir(null), "History")
            if (historyDir.exists()) {
                files = historyDir.listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
            }
        }
        isLoading = false
    }

    var selectedFileContent by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (selectedFileContent == null) "History" else selectedFileName ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedFileContent != null) {
                            selectedFileContent = null
                            selectedFileName = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = if (selectedFileContent != null) "Close details" else "Navigate back"
                        )
                    }
                },
                actions = {
                    if (selectedFileContent != null) {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(selectedFileContent ?: ""))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Copied to clipboard")
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy to clipboard"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (selectedFileContent == null) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics { contentDescription = "Loading history" }
                    )
                }
            } else if (files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No History Yet",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your recordings will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp)
                ) {
                    items(files) { file ->
                        HistoryItem(file) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val content = file.readText()
                                withContext(Dispatchers.Main) {
                                    selectedFileName = file.name
                                    selectedFileContent = content
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(selectedFileContent ?: "")
                }
            }
        }
    }
}

@Composable
fun HistoryItem(file: File, onClick: () -> Unit) {
    val lastModified = file.lastModified()
    val formattedDate = remember(lastModified) {
        val date = Date(lastModified)
        val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        format.format(date)
    }

    val length = file.length()
    val formattedSize = remember(length) {
        val kb = length / 1024
        if (kb > 1024) {
            String.format(Locale.getDefault(), "%.1f MB", kb / 1024f)
        } else {
            "$kb KB"
        }
    }

    ListItem(
        headlineContent = {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = "$formattedDate • $formattedSize",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null, // Decorative, interaction is on the row
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
