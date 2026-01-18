package space.suiten.domain.model

data class NextAction(
    val id: String,
    val aiResultId: String,
    val type: String, // dialog, books, videos
    val title: String,
    val description: String,
    val query: String? = null,
    val link: String? = null,
    val rank: Int
)
