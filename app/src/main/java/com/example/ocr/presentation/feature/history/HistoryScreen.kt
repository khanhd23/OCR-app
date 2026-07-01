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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ocr.R
import com.example.ocr.domain.model.OCRDocument
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
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        HistoryTopBar(onBack = onNavigateBack, count = documents.size)

        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            isSearching = isSearching
        )

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
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(Icons.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.history), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (count > 0) {
                Text(stringResource(R.string.documents_count, count), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSearching) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(stringResource(R.string.search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                inner()
            }
        )
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
                        MaterialTheme.colorScheme.errorContainer else Color.Transparent
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(color),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(modifier = Modifier.padding(end = 20.dp)) {
                            Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(doc.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(doc.fullText.take(80).replace("\n", " "), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniTag(stringResource(R.string.lines_count) + ": $lineCount")
                MiniTag(stringResource(R.string.words_count) + ": $wordCount")
                MiniTag(dateStr)
            }
        }

        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun MiniTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.HistoryToggleOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.no_documents), color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NoSearchResult(query: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.SearchOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.no_search_result, query), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
    }
}
