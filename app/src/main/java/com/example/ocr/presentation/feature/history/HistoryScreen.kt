package com.example.ocr.presentation.feature.history

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val documents by viewModel.documents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink900)
            .statusBarsPadding()
    ) {
        // Top bar
        HistoryTopBar(onBack = onNavigateBack, count = documents.size)

        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            isSearching = isSearching
        )

        // Content
        when {
            documents.isEmpty() && searchQuery.isBlank() -> EmptyHistoryState()
            documents.isEmpty() && searchQuery.isNotBlank() -> NoSearchResult(query = searchQuery)
            else -> DocumentList(
                documents = documents,
                onOpen = onOpenDocument,
                onDelete = { viewModel.deleteDocument(it) }
            )
        }
    }
}

@Composable
private fun HistoryTopBar(onBack: () -> Unit, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp).background(SurfaceCard, CircleShape)
        ) {
            Icon(Icons.Filled.ArrowBack, null, tint = TextPrimary)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Lịch sử quét", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (count > 0) {
                Text("$count tài liệu", color = TextHint, fontSize = 12.sp)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(TealGlow)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("$count", color = Teal400, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSearching) {
            CircularProgressIndicator(
                color = Teal400,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Outlined.Search, null, tint = TextHint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text("Tìm kiếm tài liệu...", color = TextHint, fontSize = 14.sp)
                }
                inner()
            }
        )
        if (query.isNotBlank()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Filled.Close, null, tint = TextHint, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentList(
    documents: List<OCRDocument>,
    onOpen: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = documents,
            key = { it.id }
        ) { doc ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        onDelete(doc.id)
                        true
                    } else false
                }
            )
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                        ErrorRed.copy(alpha = 0.2f) else Color.Transparent
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(color),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            modifier = Modifier.padding(end = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Delete, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Xoá", color = ErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            ) {
                DocumentCard(doc = doc, onClick = { onOpen(doc.id) })
            }
        }
    }
}

@Composable
private fun DocumentCard(doc: OCRDocument, onClick: () -> Unit) {
    val wordCount = doc.fullText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    val lineCount = doc.fullText.lines().filter { it.isNotBlank() }.size
    val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(doc.createdAt))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(TealGlow, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Description,
                null,
                tint = Teal400,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                doc.title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                doc.fullText.take(80).replace("\n", " "),
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniTag("$lineCount dòng")
                MiniTag("$wordCount từ")
                MiniTag(dateStr)
            }
        }

        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.ChevronRight, null, tint = TextHint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MiniTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Ink700)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = TextHint, fontSize = 10.sp)
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.HistoryToggleOff,
            null,
            tint = TextHint,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Chưa có tài liệu nào", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Các tài liệu đã quét sẽ xuất hiện ở đây", color = TextHint, fontSize = 13.sp)
    }
}

@Composable
private fun NoSearchResult(query: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.SearchOff, null, tint = TextHint, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Không tìm thấy kết quả", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Không có tài liệu nào khớp với \"$query\"", color = TextHint, fontSize = 13.sp)
    }
}
