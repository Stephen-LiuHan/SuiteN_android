package space.suiten.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import space.suiten.domain.model.Book
import space.suiten.domain.model.ContentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    books: List<Book>,
    onBookClick: (String) -> Unit,
    onAddContent: (contentType: ContentType, title: String, author: String?, customTypeName: String?, url: String?, memo: String?) -> Unit,
    onSettingsClick: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredBooks = books.filter { book ->
        searchQuery.isEmpty() ||
        book.title.contains(searchQuery, ignoreCase = true) ||
        (book.author?.contains(searchQuery, ignoreCase = true) == true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ライブラリ") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "追加")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("検索...") }
                )
            }

            if (filteredBooks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = if (books.isEmpty()) "コンテンツがありません" else "検索結果がありません",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (books.isEmpty()) "右下の + ボタンから本や動画を追加できます" else "別のキーワードで検索してください",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredBooks, key = { it.id }) { book ->
                    ContentCard(
                        book = book,
                        onClick = { onBookClick(book.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddContentBottomSheet(
            onDismiss = { showAddDialog = false },
            onConfirm = { contentType, title, author, customTypeName, url, memo ->
                onAddContent(contentType, title, author, customTypeName, url, memo)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ContentCard(
    book: Book,
    onClick: () -> Unit
) {
    val icon = when (book.contentType) {
        ContentType.BOOK -> Icons.Default.Book
        ContentType.VIDEO -> Icons.Default.VideoLibrary
        ContentType.SEMINAR -> Icons.Default.School
        ContentType.OTHER -> Icons.Default.Category
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (book.author != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.getTypeDisplayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContentBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (contentType: ContentType, title: String, author: String?, customTypeName: String?, url: String?, memo: String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(ContentType.BOOK) }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var customTypeName by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "コンテンツを追加",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Content Type Selection - Button style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContentTypeOption.entries.forEach { option ->
                    FilterChip(
                        selected = selectedType == option.type,
                        onClick = { selectedType = option.type },
                        label = { Text(option.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            // Custom type name for OTHER
            if (selectedType == ContentType.OTHER) {
                OutlinedTextField(
                    value = customTypeName,
                    onValueChange = { customTypeName = it },
                    label = { Text("種類名") },
                    placeholder = { Text("例: ポッドキャスト、記事") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("タイトル") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = {
                    Text(
                        when (selectedType) {
                            ContentType.BOOK -> "著者（任意）"
                            ContentType.VIDEO -> "チャンネル/作成者（任意）"
                            ContentType.SEMINAR -> "講師/主催者（任意）"
                            ContentType.OTHER -> "作成者（任意）"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL（任意）") },
                placeholder = { Text("https://...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("メモ（任意）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("キャンセル")
                }
                Button(
                    onClick = {
                        onConfirm(
                            selectedType,
                            title,
                            author.ifBlank { null },
                            if (selectedType == ContentType.OTHER) customTypeName.ifBlank { null } else null,
                            url.ifBlank { null },
                            memo.ifBlank { null }
                        )
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("追加")
                }
            }
        }
    }
}

@Composable
fun EditContentDialog(
    book: Book,
    onDismiss: () -> Unit,
    onConfirm: (title: String, author: String?, customTypeName: String?, url: String?, memo: String?) -> Unit
) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author ?: "") }
    var customTypeName by remember { mutableStateOf(book.customTypeName ?: "") }
    var url by remember { mutableStateOf(book.url ?: "") }
    var memo by remember { mutableStateOf(book.memo ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("編集") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show content type (read-only)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (book.contentType) {
                            ContentType.BOOK -> Icons.Default.Book
                            ContentType.VIDEO -> Icons.Default.VideoLibrary
                            ContentType.SEMINAR -> Icons.Default.School
                            ContentType.OTHER -> Icons.Default.Category
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = book.getTypeDisplayName(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Custom type name for OTHER
                if (book.contentType == ContentType.OTHER) {
                    OutlinedTextField(
                        value = customTypeName,
                        onValueChange = { customTypeName = it },
                        label = { Text("種類名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("タイトル") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = {
                        Text(
                            when (book.contentType) {
                                ContentType.BOOK -> "著者（任意）"
                                ContentType.VIDEO -> "チャンネル/作成者（任意）"
                                ContentType.SEMINAR -> "講師/主催者（任意）"
                                ContentType.OTHER -> "作成者（任意）"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL（任意）") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ（任意）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        title,
                        author.ifBlank { null },
                        if (book.contentType == ContentType.OTHER) customTypeName.ifBlank { null } else null,
                        url.ifBlank { null },
                        memo.ifBlank { null }
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

private enum class ContentTypeOption(
    val type: ContentType,
    val label: String,
    val icon: ImageVector
) {
    BOOK(ContentType.BOOK, "本", Icons.Default.Book),
    VIDEO(ContentType.VIDEO, "動画", Icons.Default.VideoLibrary),
    SEMINAR(ContentType.SEMINAR, "セミナー", Icons.Default.School),
    OTHER(ContentType.OTHER, "その他", Icons.Default.Category)
}
