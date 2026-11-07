package app.mixu.sample

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HorizonTest {
    @Test
    fun `reveals only what the horizon covers`() {
        val horizon = Horizon(3)
        assertTrue(horizon.reveals(3), "the boundary itself is revealed")
        assertFalse(horizon.reveals(4), "one past the boundary must stay hidden")
    }

    @Test
    fun `rejects a negative ordinal`() {
        assertFailsWith<IllegalArgumentException> { Horizon(-1) }
    }
}
