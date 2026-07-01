package com.example.ocr.presentation.feature.result

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.ocr.R
import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.presentation.component.LoadingView
import com.example.ocr.presentation.component.ResultItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultScreen(
    documentId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val copySuccess by viewModel.copySuccess.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ViewMode.Lines) }

    LaunchedEffect(documentId) { viewModel.loadDocument(documentId) }

    LaunchedEffect(exportState) {
        if (exportState is ExportState.Done) {
            val file = (exportState as ExportState.Done).file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Word File"))
            viewModel.resetExportState()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val state = uiState) {
            is ResultUiState.Loading -> LoadingView(stringResource(R.string.processing))
            is ResultUiState.Error -> ErrorView(state.message, onNavigateBack)
            is ResultUiState.Success -> {
                ResultContent(
                    document = state.document,
                    viewMode = viewMode,
                    exportState = exportState,
                    copySuccess = copySuccess,
                    onViewModeChange = { viewMode = it },
                    onCopy = { viewModel.copyText(context) },
                    onExport = { viewModel.downloadWord() },
                    onDelete = { showDeleteDialog = true },
                    onBack = onNavigateBack,
                    onHistory = onNavigateToHistory
                )
            }
        }

        AnimatedVisibility(
            visible = copySuccess,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 100.dp),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Box(modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(Color(0xFF22C55E)).padding(horizontal = 20.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.copy_text), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(stringResource(R.string.confirm_delete), color = MaterialTheme.colorScheme.onSurface) },
                text = { Text(stringResource(R.string.delete_history_msg), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteDocument { onNavigateBack() }
                    }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            )
        }
    }
}

enum class ViewMode { Lines, FullText, Image }

@Composable
private fun ResultContent(
    document: OCRDocument,
    viewMode: ViewMode,
    exportState: ExportState,
    copySuccess: Boolean,
    onViewModeChange: (ViewMode) -> Unit,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onHistory: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ResultTopBar(title = document.title, onBack = onBack, onDelete = onDelete)
        StatsRow(document = document)
        ViewModeTabs(viewMode = viewMode, onViewModeChange = onViewModeChange)
        Box(modifier = Modifier.weight(1f)) {
            when (viewMode) {
                ViewMode.Lines -> LinesView(document)
                ViewMode.FullText -> FullTextView(document)
                ViewMode.Image -> ImageView(document)
            }
        }
        BottomActions(onCopy = onCopy, onExport = onExport, onHistory = onHistory)
    }
}

@Composable
private fun ResultTopBar(title: String, onBack: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
            Icon(Icons.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(12.dp))
        Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
            Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun StatsRow(document: OCRDocument) {
    val lineCount = document.fullText.lines().filter { it.isNotBlank() }.size
    val wordCount = document.fullText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(document.createdAt))

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        StatChip(value = "$lineCount", label = stringResource(R.string.lines_count), icon = Icons.Outlined.FormatListBulleted)
        StatChip(value = "$wordCount", label = stringResource(R.string.words_count), icon = Icons.Outlined.TextFields)
        StatChip(value = dateStr, label = stringResource(R.string.created_at), icon = Icons.Outlined.CalendarToday)
    }
}

@Composable
private fun StatChip(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
}

@Composable
private fun ViewModeTabs(viewMode: ViewMode, onViewModeChange: (ViewMode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ViewMode.entries.forEach { mode ->
            val isSelected = mode == viewMode
            val label = when (mode) {
                ViewMode.Lines -> "Lines"
                ViewMode.FullText -> "Text"
                ViewMode.Image -> "Image"
            }
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { onViewModeChange(mode) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(label, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun LinesView(document: OCRDocument) {
    val lines = document.fullText.lines().filter { it.isNotBlank() }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        itemsIndexed(lines) { index, line ->
            ResultItem(lineText = line, lineNumber = index + 1)
        }
    }
}

@Composable
private fun FullTextView(document: OCRDocument) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {
        SelectionContainer {
            Text(text = document.fullText, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, lineHeight = 24.sp)
        }
    }
}

@Composable
private fun ImageView(document: OCRDocument) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        if (document.imagePath.isNotBlank()) {
            AsyncImage(model = File(document.imagePath), contentDescription = null, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.FillWidth)
        }
    }
}

@Composable
private fun BottomActions(onCopy: () -> Unit, onExport: () -> Unit, onHistory: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onCopy, 
            modifier = Modifier.weight(1f).height(50.dp), 
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.copy_text), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onExport, 
            modifier = Modifier.weight(1f).height(50.dp), 
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text(stringResource(R.string.export_word), color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onHistory, modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))) {
            Icon(Icons.Filled.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorView(message: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) { 
            Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onPrimary) 
        }
    }
}
