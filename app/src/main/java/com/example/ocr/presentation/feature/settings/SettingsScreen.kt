package com.example.ocr.presentation.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ocr.R
import com.example.ocr.domain.repository.AppTheme
import com.example.ocr.domain.repository.OCRModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val ocrModel by viewModel.ocrModel.collectAsStateWithLifecycle()
    val isHistoryEnabled by viewModel.isHistoryEnabled.collectAsStateWithLifecycle()
    
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.delete_history_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllHistory()
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = stringResource(R.string.theme)) {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.theme_mode),
                    subtitle = when (theme) {
                        AppTheme.LIGHT -> stringResource(R.string.light)
                        AppTheme.DARK -> stringResource(R.string.dark)
                        AppTheme.SYSTEM -> stringResource(R.string.system)
                    },
                    onClick = {}
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOption(
                        selected = theme == AppTheme.LIGHT,
                        label = stringResource(R.string.light),
                        onClick = { viewModel.setTheme(AppTheme.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOption(
                        selected = theme == AppTheme.DARK,
                        label = stringResource(R.string.dark),
                        onClick = { viewModel.setTheme(AppTheme.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOption(
                        selected = theme == AppTheme.SYSTEM,
                        label = stringResource(R.string.system),
                        onClick = { viewModel.setTheme(AppTheme.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.language)) {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.language),
                    subtitle = when (language) {
                        "vi" -> "Tiếng Việt"
                        "en" -> "English"
                        "zh" -> "中文"
                        "ja" -> "日本語"
                        "ko" -> "한국어"
                        "fr" -> "Français"
                        else -> "Tiếng Việt"
                    },
                    onClick = {}
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguageOption("vi", "Việt", language == "vi") { viewModel.setLanguage("vi") }
                    LanguageOption("en", "English", language == "en") { viewModel.setLanguage("en") }
                    LanguageOption("zh", "中文", language == "zh") { viewModel.setLanguage("zh") }
                    LanguageOption("ja", "日本語", language == "ja") { viewModel.setLanguage("ja") }
                    LanguageOption("ko", "한국어", language == "ko") { viewModel.setLanguage("ko") }
                    LanguageOption("fr", "Français", language == "fr") { viewModel.setLanguage("fr") }
                }
            }

            SettingsSection(title = stringResource(R.string.model)) {
                SettingsItem(
                    icon = Icons.Default.ModelTraining,
                    title = stringResource(R.string.model),
                    subtitle = when (ocrModel) {
                        OCRModel.MLKIT -> "Google ML Kit"
                        OCRModel.TROCR -> "TrOCR Transformer"
                    },
                    onClick = {}
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModelOption(
                        selected = ocrModel == OCRModel.MLKIT,
                        label = "ML Kit",
                        onClick = { viewModel.setOCRModel(OCRModel.MLKIT) },
                        modifier = Modifier.weight(1f)
                    )
                    ModelOption(
                        selected = ocrModel == OCRModel.TROCR,
                        label = "TrOCR",
                        onClick = { viewModel.setOCRModel(OCRModel.TROCR) },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.save_history), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.auto_save), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = isHistoryEnabled,
                        onCheckedChange = { viewModel.setHistoryEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.delete_all_history),
                    subtitle = "Xóa vĩnh viễn các bản quét cũ",
                    onClick = { showDeleteDialog = true }
                )
            }

            SettingsSection(title = stringResource(R.string.info)) {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.version),
                    subtitle = "1.0.0 (Internship Pro)",
                    onClick = {}
                )
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.developed_by),
                    subtitle = "Android Intern Candidate",
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ThemeOption(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageOption(code: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label, 
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ModelOption(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface)
    }
}
