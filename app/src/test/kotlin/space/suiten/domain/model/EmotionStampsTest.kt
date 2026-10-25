package space.suiten.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmotionStampsTest {
    @Test
    fun `all five stamps including empathy are available`() {
        assertEquals(listOf("💡", "🤔", "⚠️", "🔥", "❤️"), EmotionStamps.ALL.map { it.emoji })
        assertEquals("共感", EmotionStamps.ALL.last().label)
    }

    @Test
    fun `every selected stamp is stored, not just the first`() {
        assertEquals("🤔,❤️", EmotionStamps.encode(listOf("🤔", "❤️")))
        assertNull(EmotionStamps.encode(emptyList()))
    }

    @Test
    fun `decoding round-trips and tolerates a single legacy value`() {
        assertEquals(listOf("🤔", "❤️"), EmotionStamps.decode("🤔,❤️"))
        assertEquals(listOf("💡"), EmotionStamps.decode("💡"))
        assertEquals(emptyList<String>(), EmotionStamps.decode(null))
    }

    @Test
    fun `all five stamps fit the server column`() {
        val all = EmotionStamps.encode(EmotionStamps.ALL.map { it.emoji })!!
        // captures.emotion is VARCHAR(32), counted in code points
        org.junit.Assert.assertTrue(all.codePointCount(0, all.length) <= 32)
    }

    @Test
    fun `labels and display`() {
        assertEquals("疑問・共感", EmotionStamps.labels("🤔,❤️"))
        assertEquals("🤔 ❤️", EmotionStamps.display("🤔,❤️"))
    }
}
