package space.suiten.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import space.suiten.domain.model.NextAction

@Entity(
    tableName = "next_actions",
    foreignKeys = [
        ForeignKey(
            entity = AIResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["aiResultId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("aiResultId")]
)
data class NextActionEntity(
    @PrimaryKey
    val id: String,
    val aiResultId: String,
    val type: String, // dialog/books/videos
    val title: String,
    val description: String,
    val query: String?,
    val link: String?,
    val rank: Int
) {
    fun toDomain(): NextAction = NextAction(
        id = id,
        aiResultId = aiResultId,
        type = type,
        title = title,
        description = description,
        query = query,
        link = link,
        rank = rank
    )

    companion object {
        fun fromDomain(nextAction: NextAction): NextActionEntity = NextActionEntity(
            id = nextAction.id,
            aiResultId = nextAction.aiResultId,
            type = nextAction.type,
            title = nextAction.title,
            description = nextAction.description,
            query = nextAction.query,
            link = nextAction.link,
            rank = nextAction.rank
        )
    }
}
