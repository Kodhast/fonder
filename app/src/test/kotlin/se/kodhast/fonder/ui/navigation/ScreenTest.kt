package se.kodhast.fonder.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.kodhast.fonder.R

/**
 * Regressionstest för NAV-1/NAV-7/NAV-9 — flikradens fyra flikar, startskärmen och vilka underskärmar som
 * får den lilla toppraden av navigationen ([subPageTitle]) och vilka som ritar sin egen på en ram. Ren
 * datakontroll, inget Compose/Hilt behövs.
 */
class ScreenTest {

    @Test
    fun `flikraden har fyra flikar i rätt ordning med Översikt först`() {
        assertEquals(listOf(OversiktKey, InnehavKey, BeslutKey, HandelserKey), TOP_LEVEL)
        assertEquals(Screen.Oversikt, Screen.START)
        assertEquals(OversiktKey, TOP_LEVEL.first())
    }

    @Test
    fun `underskärmarna utan ram har rubrik`() {
        assertEquals(R.string.import_title, subPageTitle(ImportHoldingsKey))
        assertEquals(R.string.import_orders_title, subPageTitle(ImportOrdersKey))
        assertEquals(R.string.facit_title, subPageTitle(FacitKey))
        assertEquals(R.string.switch_watch_title, subPageTitle(SwitchWatchKey(7)))
    }

    @Test
    fun `flikarna och skärmarna på en ram får ingen topprad av navigationen`() {
        TOP_LEVEL.forEach { assertNull(subPageTitle(it)) }
        listOf(
            FondDetaljKey("SHB0000442"),
            FundSearchKey,
            TransactionFormKey(),
            SettingsKey,
            RiskProfileKey,
            BenchmarkPickerKey,
            SwitchWatchPickerKey(7),
            ComponentGalleryKey
        ).forEach { assertNull("$it", subPageTitle(it)) }
    }
}
