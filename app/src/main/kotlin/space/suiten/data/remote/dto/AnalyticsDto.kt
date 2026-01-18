package space.suiten.data.remote.dto

import com.google.gson.annotations.SerializedName

// === Personal Analytics ===
data class PersonalAnalyticsResponse(
    @SerializedName("period") val period: String,
    @SerializedName("axis_distribution") val axisDistribution: AxisDistribution,
    @SerializedName("action_stats") val actionStats: ActionStats,
    @SerializedName("feedback_stats") val feedbackStats: FeedbackStats,
    @SerializedName("insights") val insights: List<String>
)

data class AxisDistribution(
    @SerializedName("risk") val risk: Int,
    @SerializedName("success_rate") val successRate: Int,
    @SerializedName("rationale") val rationale: Int,
    @SerializedName("concept") val concept: Int
)

data class ActionStats(
    @SerializedName("dialog") val dialog: ActionStat,
    @SerializedName("books") val books: ActionStat,
    @SerializedName("videos") val videos: ActionStat
)

data class ActionStat(
    @SerializedName("shown") val shown: Int,
    @SerializedName("opened") val opened: Int,
    @SerializedName("completed") val completed: Int
)

data class FeedbackStats(
    @SerializedName("average_helpfulness") val averageHelpfulness: Double,
    @SerializedName("distribution") val distribution: Map<String, Int>
)

// === Global Analytics ===
data class GlobalAnalyticsResponse(
    @SerializedName("period") val period: String,
    @SerializedName("total_users") val totalUsers: Int,
    @SerializedName("axis_distribution") val axisDistribution: Map<String, Double>,
    @SerializedName("action_open_rates") val actionOpenRates: Map<String, Double>,
    @SerializedName("feedback_average") val feedbackAverage: Double
)

// === Trends ===
data class TrendsResponse(
    @SerializedName("metric") val metric: String,
    @SerializedName("period") val period: String,
    @SerializedName("data_points") val dataPoints: List<TrendDataPoint>
)

data class TrendDataPoint(
    @SerializedName("date") val date: String,
    @SerializedName("values") val values: Map<String, Int>
)
