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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.presentation.component.LoadingView
import com.example.ocr.presentation.component.ResultItem
import com.example.ocr.presentation.theme.*
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

    // Handle export done -> share file
    LaunchedEffect(exportState) {
        if (exportState is ExportState.Done) {
            val file = (exportState as ExportState.Done).file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ file Word"))
            viewModel.resetExportState()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink900)) {
        when (val state = uiState) {
            is ResultUiState.Loading -> LoadingView("Đang tải kết quả...")
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

        // Toast-style copy feedback
        AnimatedVisibility(
            visible = copySuccess,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 100.dp),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(SuccessGreen)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, null, tint = White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Đã sao chép!", color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }

        // Export loading overlay
        if (exportState is ExportState.Downloading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
                LoadingView("Đang tải file Word từ server...")
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = SurfaceCard,
                title = { Text("Xoá tài liệu?", color = TextPrimary) },
                text = { Text("Hành động này không thể hoàn tác.", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteDocument { onNavigateBack() }
                    }) { Text("Xoá", color = ErrorRed, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Huỷ", color = TextSecondary) }
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
        // Top app bar
        ResultTopBar(
            title = document.title,
            onBack = onBack,
            onDelete = onDelete
        )

        // Stats row
        StatsRow(document = document)

        // View mode tabs
        ViewModeTabs(viewMode = viewMode, onViewModeChange = onViewModeChange)

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when (viewMode) {
                ViewMode.Lines -> LinesView(document)
                ViewMode.FullText -> FullTextView(document)
                ViewMode.Image -> ImageView(document)
            }
        }

        // Bottom action bar
        BottomActions(
            onCopy = onCopy,
            onExport = onExport,
            onHistory = onHistory
        )
    }
}

@Composable
private fun ResultTopBar(title: String, onBack: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp).background(SurfaceCard, CircleShape)
        ) {
            Icon(Icons.Filled.ArrowBack, null, tint = TextPrimary)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(40.dp).background(SurfaceCard, CircleShape)
        ) {
            Icon(Icons.Outlined.Delete, null, tint = ErrorRed)
        }
    }
}

@Composable
private fun StatsRow(document: OCRDocument) {
    val lineCount = document.ocrResult?.lines?.size ?: document.fullText.lines().size
    val wordCount = document.fullText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        .format(Date(document.createdAt))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatChip(value = "$lineCount", label = "dòng", icon = Icons.Outlined.FormatListBulleted)
        VerticalDivider()
        StatChip(value = "$wordCount", label = "từ", icon = Icons.Outlined.TextFields)
        VerticalDivider()
        StatChip(value = dateStr, label = "ngày tạo", icon = Icons.Outlined.CalendarToday)
    }
}

@Composable
private fun StatChip(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Teal400, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, color = TextHint, fontSize = 10.sp)
    }
}

@Composable
private fun VerticalDivider() {
    Box(modifier = Modifier.width(1.dp).height(36.dp).background(DividerColor))
}

@Composable
private fun ViewModeTabs(viewMode: ViewMode, onViewModeChange: (ViewMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ViewMode.values().forEach { mode ->
            val isSelected = mode == viewMode
            val label = when (mode) {
                ViewMode.Lines -> "Từng dòng"
                ViewMode.FullText -> "Toàn văn"
                ViewMode.Image -> "Hình ảnh"
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Teal400 else Color.Transparent)
                    .clickable { onViewModeChange(mode) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSelected) Ink900 else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun LinesView(document: OCRDocument) {
    val lines = document.ocrResult?.lines?.map { it.text }
        ?: document.fullText.lines().filter { it.isNotBlank() }

    if (lines.isEmpty()) {
        EmptyState("Không tìm thấy dòng văn bản")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(lines) { index, line ->
            ResultItem(lineText = line, lineNumber = index + 1)
        }
    }
}

@Composable
private fun FullTextView(document: OCRDocument) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceCard)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            SelectionContainer {
                Text(
                    text = document.fullText.ifBlank { "Không có văn bản" },
                    color = if (document.fullText.isBlank()) TextHint else TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 24.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ImageView(document: OCRDocument) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (document.imagePath.isNotBlank()) {
            AsyncImage(
                model = File(document.imagePath),
                contentDescription = "Ảnh tài liệu",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.FillWidth
            )
        } else {
            EmptyState("Không có hình ảnh")
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Inbox, null, tint = TextHint, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, color = TextHint, fontSize = 14.sp)
        }
    }
}

@Composable
private fun BottomActions(onCopy: () -> Unit, onExport: () -> Unit, onHistory: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Copy
        OutlinedButton(
            onClick = onCopy,
            modifier = Modifier.weight(1f).height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal400),
            border = BorderStroke(1.5.dp, Teal400),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Sao chép", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }

        // Export Word
        Button(
            onClick = onExport,
            modifier = Modifier.weight(1f).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber400, contentColor = Ink900),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.Description, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Xuất Word",color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        // History shortcut
        IconButton(
            onClick = onHistory,
            modifier = Modifier
                .size(50.dp)
                .background(SurfaceCard, RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.Filled.History, null, tint = TextSecondary)
        }
    }
}

@Composable
private fun ErrorView(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, color = TextPrimary, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Teal400, contentColor = Ink900),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Quay lại") }
    }
}
