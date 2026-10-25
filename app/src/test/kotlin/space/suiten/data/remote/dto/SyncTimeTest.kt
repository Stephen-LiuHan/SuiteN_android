package space.suiten.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyncTimeTest {
    private val millis = 1_761_393_600_123L // 2025-10-25T12:00:00.123Z
    private val seconds = 1_761_393_600L

    @Test
    fun `local milliseconds are sent as API seconds`() {
        assertEquals(seconds, SyncTime.toApi(millis))
        assertNull(SyncTime.toApi(null as Long?))
    }

    @Test
    fun `API seconds are stored as milliseconds`() {
        assertEquals(seconds * 1000, SyncTime.fromApi(seconds))
    }

    @Test
    fun `values already in milliseconds are kept`() {
        assertEquals(millis, SyncTime.fromApi(millis))
    }
}
