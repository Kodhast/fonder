package se.kodhast.fonder.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regressionstest för NAV-1/NAV-7 — flikradens fyra flikar och startskärmen. Ren datakontroll, inget
 * Compose/Hilt behövs. Att varje skärm ritar sin egen topprad (#266 F6) prövas i `FonderNavHostTest`.
 */
class ScreenTest {

    @Test
    fun `flikraden har fyra flikar i rätt ordning med Översikt först`() {
        assertEquals(listOf(OversiktKey, InnehavKey, BeslutKey, HandelserKey), TOP_LEVEL)
        assertEquals(Screen.Oversikt, Screen.START)
        assertEquals(OversiktKey, TOP_LEVEL.first())
    }
}
