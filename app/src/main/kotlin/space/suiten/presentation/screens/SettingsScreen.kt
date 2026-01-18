package space.suiten.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import space.suiten.domain.model.Book
import space.suiten.presentation.viewmodel.SyncUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    books: List<Book>,
    syncUiState: SyncUiState,
    onBack: () -> Unit,
    onDeleteAllData: () -> Unit,
    onDeleteBookData: (String) -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onLogout: () -> Unit,
    onSync: () -> Unit,
    onClearError: () -> Unit
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showDeleteBookDialog by remember { mutableStateOf(false) }
    var selectedBookForDeletion by remember { mutableStateOf<Book?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sync Section
            SyncSection(
                syncUiState = syncUiState,
                onLoginClick = { showAuthDialog = true },
                onLogout = onLogout,
                onSync = onSync
            )

            Divider()

            // Data Management Section
            DataManagementSection(
                books = books,
                onDeleteAllClick = { showDeleteAllDialog = true },
                onDeleteBookClick = { book ->
                    selectedBookForDeletion = book
                    showDeleteBookDialog = true
                }
            )

            Divider()

            // Privacy Section
            PrivacySection()

            // App Info Section
            AppInfoSection()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Auth Dialog
    if (showAuthDialog) {
        AuthDialog(
            isLoading = syncUiState.isLoading,
            error = syncUiState.error,
            onDismiss = {
                showAuthDialog = false
                onClearError()
            },
            onLogin = { email, password ->
                onLogin(email, password)
            },
            onRegister = { email, password ->
                onRegister(email, password)
            }
        )

        // Close dialog on successful login
        LaunchedEffect(syncUiState.isLoggedIn) {
            if (syncUiState.isLoggedIn) {
                showAuthDialog = false
            }
        }
    }

    // Show error snackbar
    if (syncUiState.error != null && !showAuthDialog) {
        LaunchedEffect(syncUiState.error) {
            // Error will be shown in auth dialog or cleared
        }
    }

    // Delete All Data Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("すべてのデータを削除") },
            text = {
                Text("本棚、キャプチャ、AI分析結果、プロフィールデータがすべて削除されます。この操作は取り消せません。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllData()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("削除する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // Delete Book Data Confirmation Dialog
    if (showDeleteBookDialog && selectedBookForDeletion != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteBookDialog = false
                selectedBookForDeletion = null
            },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("本のデータを削除") },
            text = {
                Text("「${selectedBookForDeletion?.title}」に関連するすべてのキャプチャとAI分析結果が削除されます。この操作は取り消せません。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedBookForDeletion?.let { onDeleteBookData(it.id) }
                        showDeleteBookDialog = false
                        selectedBookForDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("削除する")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteBookDialog = false
                    selectedBookForDeletion = null
                }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun DataManagementSection(
    books: List<Book>,
    onDeleteAllClick: () -> Unit,
    onDeleteBookClick: (Book) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "データ管理",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Delete by book
        if (books.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "本ごとにデータを削除",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "特定の本に関連するキャプチャとAI分析結果を削除します",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    books.forEach { book ->
                        OutlinedButton(
                            onClick = { onDeleteBookClick(book) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (book.author != null) {
                                        Text(
                                            text = book.author,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "削除",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delete all data
        Button(
            onClick = onDeleteAllClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("すべてのデータを削除")
        }
    }
}

@Composable
private fun PrivacySection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "プライバシー保護",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PrivacyItem(
                    title = "OCRテキストは保存されません",
                    description = "スクリーンショットから抽出されたテキストは、AI処理中のみメモリに保持され、処理後すぐに破棄されます。データベースやログには一切保存されません。"
                )

                Divider()

                PrivacyItem(
                    title = "原文の再現はしません",
                    description = "AIの出力はあなたの気づきや反応の整理に限定されます。本の原文やそれに近い内容は出力されません。"
                )

                Divider()

                PrivacyItem(
                    title = "データはローカルに保存",
                    description = "すべてのデータはお使いのデバイス内にのみ保存されます。外部サーバーへの自動同期は行いません。"
                )
            }
        }
    }
}

@Composable
private fun PrivacyItem(
    title: String,
    description: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppInfoSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "アプリ情報",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "バージョン",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "アプリ名",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "SuiteN",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncSection(
    syncUiState: SyncUiState,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit,
    onSync: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "クラウド同期",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (syncUiState.isLoggedIn) {
                    // Logged in state
                    Text(
                        text = "ログイン済み",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Last sync time
                    syncUiState.lastSyncAt?.let { timestamp ->
                        if (timestamp > 0) {
                            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                            val formattedDate = dateFormat.format(Date(timestamp * 1000))
                            Text(
                                text = "最終同期: $formattedDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Sync button
                    Button(
                        onClick = onSync,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !syncUiState.isSyncing,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (syncUiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("同期中...")
                        } else {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("今すぐ同期")
                        }
                    }

                    // Logout button
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !syncUiState.isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ログアウト")
                    }
                } else {
                    // Not logged in state
                    Text(
                        text = "アカウントにログインしてデータをクラウドにバックアップしましょう",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ログイン / 新規登録")
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthDialog(
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isLoginMode) "ログイン" else "新規登録")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("メールアドレス") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("パスワード") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                TextButton(
                    onClick = { isLoginMode = !isLoginMode },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        if (isLoginMode) "アカウントをお持ちでない方" else "既にアカウントをお持ちの方"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isLoginMode) {
                        onLogin(email, password)
                    } else {
                        onRegister(email, password)
                    }
                },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isLoginMode) "ログイン" else "登録")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
