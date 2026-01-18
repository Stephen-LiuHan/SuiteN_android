package space.suiten.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import space.suiten.domain.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    isPersonalizationEnabled: Boolean,
    onTogglePersonalization: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("あなたの傾向") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header description
            Text(
                text = "直近30件の分析結果から、あなたの思考傾向を可視化しています",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Axis Preferences Card
            AxisPreferencesCard(
                preferences = userProfile?.axisPreferences ?: emptyMap()
            )

            // Action Preferences Card
            ActionPreferencesCard(
                preferences = userProfile?.actionPreferences ?: emptyMap()
            )

            // Personalization Toggle Card
            PersonalizationCard(
                isEnabled = isPersonalizationEnabled,
                onToggle = onTogglePersonalization
            )

            // Insights Section
            if (userProfile != null && userProfile.axisPreferences.isNotEmpty()) {
                InsightsCard(
                    axisPreferences = userProfile.axisPreferences
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AxisPreferencesCard(
    preferences: Map<String, Int>
) {
    val axisLabels = mapOf(
        "risk" to "リスク",
        "success_rate" to "成功率",
        "rationale" to "あるべき論の根拠",
        "concept" to "コンセプトと意義"
    )

    val total = preferences.values.sum().coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "思考の軸の傾向",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (preferences.isEmpty()) {
                EmptyStateMessage(
                    message = "まだデータがありません。\nキャプチャを分析すると傾向が表示されます。"
                )
            } else {
                axisLabels.forEach { (key, label) ->
                    val count = preferences[key] ?: 0
                    val percentage = (count.toFloat() / total * 100).toInt()
                    AxisProgressBar(
                        label = label,
                        percentage = percentage,
                        count = count
                    )
                }
            }
        }
    }
}

@Composable
private fun AxisProgressBar(
    label: String,
    percentage: Int,
    count: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$percentage% ($count)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = percentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun ActionPreferencesCard(
    preferences: Map<String, Int>
) {
    val actionConfig = listOf(
        Triple("dialog", "AIと深掘り", Icons.Default.Chat),
        Triple("books", "関連書籍", Icons.Default.Book),
        Triple("videos", "関連動画", Icons.Default.PlayCircle)
    )

    val total = preferences.values.sum().coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "アクション傾向",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (preferences.isEmpty()) {
                EmptyStateMessage(
                    message = "まだデータがありません。\nNext Actionsを実行すると傾向が表示されます。"
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    actionConfig.forEach { (key, label, icon) ->
                        val count = preferences[key] ?: 0
                        val percentage = (count.toFloat() / total * 100).toInt()
                        ActionStatCard(
                            icon = icon,
                            label = label,
                            percentage = percentage,
                            count = count
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionStatCard(
    icon: ImageVector,
    label: String,
    percentage: Int,
    count: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${count}回",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PersonalizationCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "パーソナライズ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "傾向に基づいてNext Actionsを最適化します",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun InsightsCard(
    axisPreferences: Map<String, Int>
) {
    val dominantAxis = axisPreferences.maxByOrNull { it.value }?.key
    val insight = when (dominantAxis) {
        "risk" -> "リスク重視型：失敗を避け、慎重に判断する傾向があります"
        "success_rate" -> "成功率重視型：実現可能性を重視する傾向があります"
        "rationale" -> "論理重視型：根拠や前提を大切にする傾向があります"
        "concept" -> "概念重視型：本質や意義を追求する傾向があります"
        else -> null
    }

    if (insight != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "あなたの思考スタイル",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
