package space.suiten.data.local.dao

import androidx.room.*
import space.suiten.data.local.entity.NextActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NextActionDao {
    @Query("SELECT * FROM next_actions WHERE aiResultId = :aiResultId ORDER BY rank ASC")
    fun getActionsByResultId(aiResultId: String): Flow<List<NextActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<NextActionEntity>)

    @Delete
    suspend fun deleteAction(action: NextActionEntity)

    @Query("DELETE FROM next_actions WHERE aiResultId = :aiResultId")
    suspend fun deleteActionsByResultId(aiResultId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNextAction(action: NextActionEntity)

    @Query("DELETE FROM next_actions WHERE id = :id")
    suspend fun deleteNextActionById(id: String)
}
