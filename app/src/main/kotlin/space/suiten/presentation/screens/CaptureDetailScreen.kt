package space.suiten.presentation.screens

import space.suiten.domain.model.EmotionStamps
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import space.suiten.domain.model.AIStatus
import space.suiten.domain.model.Capture
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureDetailScreen(
    capture: Capture?,
    isReanalyzing: Boolean = false,
    ocrText: String? = null,
    isExtractingText: Boolean = false,
    onBack: () -> Unit,
    onViewAIResult: (String) -> Unit,
    onReanalyze: (String) -> Unit,
    onExtractText: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReanalyzeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("キャプチャ詳細") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "削除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (capture == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image Preview
                ImagePreviewCard(imagePath = capture.imagePath)

                // OCR Text Extraction (only if image exists)
                if (capture.imagePath.isNotBlank()) {
                    OCRTextSection(
                        ocrText = ocrText,
                        isExtracting = isExtractingText,
                        onExtract = { onExtractText(capture.imagePath) }
                    )
                }

                // Emotion and Memo
                EmotionMemoCard(
                    emotion = capture.emotion,
                    memo = capture.userMemo
                )

                // Status and Date
                StatusInfoCard(
                    status = capture.aiStatus,
                    createdAt = capture.createdAt
                )

                // AI Result Buttons
                when {
                    isReanalyzing || capture.aiStatus == AIStatus.RUNNING -> {
                        Button(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI分析中...")
                        }
                    }
                    capture.aiStatus == AIStatus.DONE -> {
                        Button(
                            onClick = { onViewAIResult(capture.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("AI分析結果を見る")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showReanalyzeDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("もう一度AI分析する")
                        }
                    }
                    capture.aiStatus == AIStatus.PENDING || capture.aiStatus == AIStatus.ERROR -> {
                        Button(
                            onClick = { onViewAIResult(capture.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("AI分析を開始する")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Delete Button
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("このキャプチャを削除")
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && capture != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("キャプチャを削除") },
            text = { Text("このキャプチャを削除しますか？この操作は取り消せません。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(capture.id)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // Reanalyze Confirmation Dialog
    if (showReanalyzeDialog && capture != null) {
        AlertDialog(
            onDismissRequest = { showReanalyzeDialog = false },
            title = { Text("再分析") },
            text = { Text("AI分析をやり直しますか？現在の分析結果は削除されます。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReanalyzeDialog = false
                        onReanalyze(capture.id)
                    }
                ) {
                    Text("再分析する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReanalyzeDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun ImagePreviewCard(imagePath: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (imagePath.isNotBlank()) {
            AsyncImage(
                model = Uri.fromFile(File(imagePath)),
                contentDescription = "キャプチャ画像",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "画像なし",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmotionMemoCard(
    emotion: String?,
    memo: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emotion
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "感情",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (emotion != null) {
                    Text(
                        text = EmotionStamps.display(emotion),
                        fontSize = 32.sp
                    )
                    Text(
                        text = EmotionStamps.labels(emotion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "未設定",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Divider()

            // Memo
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "メモ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = memo ?: "メモなし",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (memo != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StatusInfoCard(
    status: AIStatus,
    createdAt: java.util.Date
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status
            Column {
                Text(
                    text = "AI分析ステータス",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = status)
            }

            // Date
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "作成日時",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
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
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun OCRTextSection(
    ocrText: String?,
    isExtracting: Boolean,
    onExtract: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "抽出テキスト",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                isExtracting -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "テキストを抽出中...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                ocrText != null -> {
                    if (ocrText.isBlank()) {
                        Text(
                            text = "テキストを抽出できませんでした",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = ocrText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = onExtract,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("画像からテキストを抽出")
                    }
                }
            }
        }
    }
}
