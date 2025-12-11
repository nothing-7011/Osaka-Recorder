package cn.mapleisle.osaka.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (selectedFileContent == null) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp)
                ) {
                    if (files.isEmpty()) {
                        item {
                            Text("No history found.", modifier = Modifier.padding(16.dp))
                        }
                    }
                    items(files) { file ->
                        HistoryItem(file) {
                            // Read content asynchronously too if files are large, but for now simple text reading is okay
                            // or better, wrap it too
                            selectedFileName = file.name
                            selectedFileContent = file.readText()
                        }
                        Divider()
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
                Text(selectedFileContent ?: "")
            }
        }
    }
}

@Composable
fun HistoryItem(file: File, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(file.name) },
        supportingContent = { Text("${file.length() / 1024} KB") },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
