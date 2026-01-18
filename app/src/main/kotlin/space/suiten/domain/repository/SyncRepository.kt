package space.suiten.domain.repository

import kotlinx.coroutines.flow.Flow
import space.suiten.data.remote.dto.SyncConflict
import space.suiten.data.remote.dto.SyncStatusResponse

data class SyncResult(
    val success: Boolean,
    val syncedAt: Long?,
    val conflicts: List<SyncConflict> = emptyList(),
    val error: String? = null
)

interface SyncRepository {
    val isSyncing: Flow<Boolean>
    val lastSyncAt: Flow<Long?>

    suspend fun sync(): SyncResult
    suspend fun getSyncStatus(): Result<SyncStatusResponse>
    suspend fun forceFullSync(): SyncResult
}
