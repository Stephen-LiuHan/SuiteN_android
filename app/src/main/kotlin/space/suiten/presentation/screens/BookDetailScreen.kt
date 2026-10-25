package space.suiten.presentation.screens

import space.suiten.domain.model.EmotionStamps
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VideoLibrary
import space.suiten.domain.model.ContentType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.suiten.domain.model.Book
import space.suiten.domain.model.BookSummaryActions
import space.suiten.domain.model.Capture
import space.suiten.domain.model.AIStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    book: Book?,
    captures: List<Capture>,
    isGeneratingSummary: Boolean,
    summaryError: String?,
    onBack: () -> Unit,
    onCaptureClick: (String) -> Unit,
    onAddCapture: () -> Unit,
    onGenerateSummary: () -> Unit,
    onSaveSummary: () -> Unit,
    onEditSummary: (String) -> Unit,
    onRateSummary: (Int) -> Unit,
    onEditBook: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = onEditBook) {
                        Icon(Icons.Default.Edit, contentDescription = "編集")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCapture,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("気づきを追加") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Book Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    // Content type badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = when (book?.contentType) {
                                ContentType.BOOK -> Icons.Default.Book
                                ContentType.VIDEO -> Icons.Default.VideoLibrary
                                ContentType.SEMINAR -> Icons.Default.School
                                ContentType.OTHER -> Icons.Default.Category
                                null -> Icons.Default.Book
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = book?.getTypeDisplayName() ?: "本",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = book?.title ?: "",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (book?.author != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = book.author,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // URL
                    if (book?.url != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = book.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }

                    // Memo
                    if (book?.memo != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = book.memo,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Divider()
            }

            // Book Summary Section
            item {
                BookSummarySection(
                    book = book,
                    isGenerating = isGeneratingSummary,
                    error = summaryError,
                    captureCount = captures.size,
                    onGenerateSummary = onGenerateSummary,
                    onSaveSummary = onSaveSummary,
                    onEditSummary = onEditSummary,
                    onRateSummary = onRateSummary
                )
            }

            // Captures Section
            if (captures.isEmpty()) {
                item {
                    EmptyCaptures(onAddCapture = onAddCapture)
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "キャプチャ一覧",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${captures.size}件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(captures, key = { it.id }) { capture ->
                    CaptureListItem(
                        capture = capture,
                        onClick = { onCaptureClick(capture.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCaptures(onAddCapture: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "キャプチャがありません",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "スクリーンショットを追加して\nあなたの気づきを記録しましょう",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CaptureListItem(
    capture: Capture,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emotion
            if (capture.emotion != null) {
                Text(
                    text = EmotionStamps.display(capture.emotion),
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = capture.userMemo ?: "メモなし",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2
                )
            }

            // Status
            StatusBadge(status = capture.aiStatus)
        }
    }
}

@Composable
private fun StatusBadge(status: AIStatus) {
    val (text, containerColor, contentColor) = when (status) {
        AIStatus.PENDING -> Triple("未処理", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        AIStatus.RUNNING -> Triple("処理中", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        AIStatus.DONE -> Triple("完了", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        AIStatus.ERROR -> Triple("エラー", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StatusChip(text: String, containerColor: androidx.compose.ui.graphics.Color, contentColor: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BookSummarySection(
    book: Book?,
    isGenerating: Boolean,
    error: String?,
    captureCount: Int,
    onGenerateSummary: () -> Unit,
    onSaveSummary: () -> Unit,
    onEditSummary: (String) -> Unit,
    onRateSummary: (Int) -> Unit
) {
    val summary = book?.summary
    var isExpanded by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "この本のまとめ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (book?.summaryCompletedAt != null) {
                        StatusChip(text = "完成", containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "閉じる" else "開く",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Collapsible content
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (error != null) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    when {
                        isGenerating -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "キャプチャを横断してまとめています...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        summary != null && book != null -> {
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            SummaryRating(
                                selected = book.summaryHelpfulness,
                                onRate = onRateSummary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = onGenerateSummary) {
                                    Text("作り直す")
                                }
                                OutlinedButton(onClick = { isEditing = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("編集")
                                }
                                if (book.summarySavedAt == null) {
                                    Button(onClick = onSaveSummary) {
                                        Text("保存する")
                                    }
                                } else {
                                    Text(
                                        text = "保存済み",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if ((book.summaryCaptureCount ?: 0) < BookSummaryActions.MIN_CAPTURES) {
                                Text(
                                    text = "キャプチャが${BookSummaryActions.MIN_CAPTURES}件以上あると、疑問と共感が並んで、考え方が見えやすくなります。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        captureCount > 0 -> {
                            Text(
                                text = "${captureCount}件のキャプチャを横断して、この本を通したあなたの読み方をAIがまとめます。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onGenerateSummary,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("この本をまとめる")
                            }
                        }
                        else -> {
                            Text(
                                text = "キャプチャを追加すると、一冊を通したあなたの読み方をAIがまとめます。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (isEditing && summary != null) {
        EditSummaryDialog(
            initial = summary,
            onDismiss = { isEditing = false },
            onConfirm = { text ->
                onEditSummary(text)
                isEditing = false
            }
        )
    }
}

private val summaryRatings = listOf(1 to "いまいち", 2 to "まあまあ", 3 to "とても")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryRating(
    selected: Int?,
    onRate: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "このまとめは役に立ちましたか？",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            summaryRatings.forEach { (value, label) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onRate(value) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun EditSummaryDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("まとめを編集") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) {
                Text("保存する")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
