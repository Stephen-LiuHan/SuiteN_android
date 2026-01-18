package space.suiten.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import space.suiten.domain.model.AIResult
import space.suiten.domain.model.NextAction

data class AxisOption(
    val id: String,
    val label: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIResultScreen(
    captureId: String,
    aiResult: AIResult?,
    nextActions: List<NextAction>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onChatClick: (String) -> Unit,
    onAxisSelect: (String) -> Unit,
    onNextActionClick: (NextAction) -> Unit,
    onFeedback: (Int) -> Unit
) {
    val axisOptions = listOf(
        AxisOption("risk", "リスク", "失敗パターン・ガードレールの特定"),
        AxisOption("success_rate", "成功率", "成功条件の分解・検証計画"),
        AxisOption("rationale", "あるべき論の根拠", "前提整理・反証可能性・代替案"),
        AxisOption("concept", "コンセプトと意義", "目的再定義・適用範囲・価値仮説")
    )

    var selectedAxis by remember { mutableStateOf(aiResult?.axisSelected) }
    var feedbackGiven by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI分析結果") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "AI分析中...",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "画像からテキストを抽出し、\n本についての気づきを整理しています",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else if (aiResult == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("分析結果がありません")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Draft Summary Section
                DraftSummaryCard(
                    summary = aiResult.draftSummary,
                    question = aiResult.deepeningQuestion
                )

                // 2. 4-Axis Selection
                AxisSelectionSection(
                    options = axisOptions,
                    selectedAxis = selectedAxis,
                    onSelect = { axis ->
                        selectedAxis = axis
                        onAxisSelect(axis)
                    }
                )

                // 3. Next Actions
                if (nextActions.isNotEmpty()) {
                    NextActionsSection(
                        actions = nextActions,
                        onActionClick = onNextActionClick,
                        onChatClick = { resultId ->
                            onChatClick(resultId)
                        },
                        aiResultId = aiResult.id
                    )
                }

                // 4. Topic Tags
                if (aiResult.topicTags.isNotEmpty()) {
                    TopicTagsSection(tags = aiResult.topicTags)
                }

                // 5. Feedback Section
                FeedbackSection(
                    feedbackGiven = feedbackGiven,
                    onFeedback = { rating ->
                        feedbackGiven = rating
                        onFeedback(rating)
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DraftSummaryCard(
    summary: String,
    question: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "本についての気づき",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Divider()

            // Deepening Question
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "深掘りの視点",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AxisSelectionSection(
    options: List<AxisOption>,
    selectedAxis: String?,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "どの観点で深掘りしますか？",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        options.forEach { option ->
            val isSelected = selectedAxis == option.id
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option.id) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NextActionsSection(
    actions: List<NextAction>,
    onActionClick: (NextAction) -> Unit,
    onChatClick: (String) -> Unit,
    aiResultId: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "次のアクション",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        actions.forEach { action ->
            NextActionCard(
                action = action,
                onClick = {
                    if (action.type == "dialog") {
                        onChatClick(aiResultId)
                    } else {
                        onActionClick(action)
                    }
                }
            )
        }
    }
}

@Composable
private fun NextActionCard(
    action: NextAction,
    onClick: () -> Unit
) {
    val (icon, iconColor) = when (action.type) {
        "dialog" -> Icons.Default.Chat to MaterialTheme.colorScheme.primary
        "books" -> Icons.Default.Book to MaterialTheme.colorScheme.secondary
        "videos" -> Icons.Default.PlayCircle to MaterialTheme.colorScheme.tertiary
        else -> Icons.Default.AutoAwesome to MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TopicTagsSection(tags: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "トピック",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            tags.take(5).forEach { tag ->
                SuggestionChip(
                    onClick = { },
                    label = { Text(tag) }
                )
            }
        }
    }
}

@Composable
private fun FeedbackSection(
    feedbackGiven: Int?,
    onFeedback: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "この分析は役に立ちましたか？",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeedbackButton(
                    label = "微妙",
                    rating = 1,
                    isSelected = feedbackGiven == 1,
                    onClick = { onFeedback(1) }
                )
                FeedbackButton(
                    label = "まあまあ",
                    rating = 2,
                    isSelected = feedbackGiven == 2,
                    onClick = { onFeedback(2) }
                )
                FeedbackButton(
                    label = "役立った",
                    rating = 3,
                    isSelected = feedbackGiven == 3,
                    onClick = { onFeedback(3) }
                )
            }
            if (feedbackGiven != null) {
                Text(
                    text = "フィードバックありがとうございます！",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun FeedbackButton(
    label: String,
    rating: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        FilledTonalButton(onClick = onClick) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(label)
        }
    }
}
