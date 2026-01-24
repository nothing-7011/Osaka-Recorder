package cn.mapleisle.osaka.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cn.mapleisle.osaka.data.ConfigManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: (Int) -> Unit // Callback to update theme immediately in MainActivity
) {
    val context = LocalContext.current
    val configManager = remember { ConfigManager(context) }

    // State wrappers for saving
    var baseUrl by remember { mutableStateOf(configManager.baseUrl) }
    var apiKey by remember { mutableStateOf(configManager.apiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var modelName by remember { mutableStateOf(configManager.modelName) }
    var timeout by remember { mutableStateOf(configManager.timeout.toString()) }
    var retry by remember { mutableStateOf(configManager.retry.toString()) }
    var systemPrompt by remember { mutableStateOf(configManager.systemPrompt) }
    var themeMode by remember { mutableIntStateOf(configManager.themeMode) }

    // Auto-save effect or save on change?
    // For simplicity in this structure, we can save on disposal or use immediate save.
    // Let's use immediate save for better UX.

    fun saveAll() {
        configManager.baseUrl = baseUrl
        configManager.apiKey = apiKey
        configManager.modelName = modelName
        configManager.timeout = timeout.toLongOrNull() ?: 30L
        configManager.retry = retry.toIntOrNull() ?: 3
        configManager.systemPrompt = systemPrompt
        configManager.themeMode = themeMode
    }

    DisposableEffect(Unit) {
        onDispose {
            saveAll()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        saveAll()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                "Interface",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )

            // Theme Selector
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme Mode")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ThemeOption(label = "System", selected = themeMode == 0) {
                            themeMode = 0
                            configManager.themeMode = 0
                            onThemeChanged(0)
                        }
                        ThemeOption(label = "Light", selected = themeMode == 1) {
                            themeMode = 1
                            configManager.themeMode = 1
                            onThemeChanged(1)
                        }
                        ThemeOption(label = "Dark", selected = themeMode == 2) {
                            themeMode = 2
                            configManager.themeMode = 2
                            onThemeChanged(2)
                        }
                    }
                }
            }

            Text(
                "API Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL") },
                placeholder = { Text("https://generativelanguage.googleapis.com/v1beta") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                trailingIcon = if (baseUrl.isNotEmpty()) {
                    {
                        IconButton(onClick = { baseUrl = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Base URL"
                            )
                        }
                    }
                } else null
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                trailingIcon = {
                    val image = if (isApiKeyVisible)
                        Icons.Filled.Visibility
                    else
                        Icons.Filled.VisibilityOff

                    val description = if (isApiKeyVisible) "Hide password" else "Show password"

                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                }
            )

            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("Model Name") },
                placeholder = { Text("gpt-4o") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                trailingIcon = if (modelName.isNotEmpty()) {
                    {
                        IconButton(onClick = { modelName = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Model Name"
                            )
                        }
                    }
                } else null
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val timeoutValue = timeout.toLongOrNull()
                val isTimeoutError = timeout.isNotEmpty() && (timeoutValue == null || timeoutValue < 1 || timeoutValue > 300)

                OutlinedTextField(
                    value = timeout,
                    onValueChange = { if (it.all { c -> c.isDigit() }) timeout = it },
                    label = { Text("Timeout (s)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    placeholder = { Text("30") },
                    supportingText = { Text(if (isTimeoutError) "1-300s" else "Default: 30s") },
                    isError = isTimeoutError
                )

                val retryValue = retry.toIntOrNull()
                val isRetryError = retry.isNotEmpty() && (retryValue == null || retryValue < 0 || retryValue > 10)

                OutlinedTextField(
                    value = retry,
                    onValueChange = { if (it.all { c -> c.isDigit() }) retry = it },
                    label = { Text("Retry Count") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    placeholder = { Text("3") },
                    supportingText = { Text(if (isRetryError) "0-10" else "Default: 3") },
                    isError = isRetryError
                )
            }

            Text(
                "Prompt Engineering",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("System Prompt") },
                placeholder = { Text("e.g. You are a helpful assistant.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default
                ),
                trailingIcon = if (systemPrompt.isNotEmpty()) {
                    {
                        IconButton(onClick = { systemPrompt = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear System Prompt"
                            )
                        }
                    }
                } else null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        }
    )
}
