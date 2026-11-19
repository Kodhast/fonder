package se.kodhast.fonder.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import se.kodhast.ui.navigation.TabBackStack

/**
 * Fonders back stack (NAV-7) – android-ui:s [TabBackStack] med Fonders flikar och nycklar: Översikt
 * underst, flikbyte som minns flikens stack, bakåt via Översikt, att Fonders alla nycklar går att spara och
 * läsa tillbaka (rotation och processdöd) och att ett läge sparat av en äldre version ger startläget. Själva
 * stackens regler testas i biblioteket; här bara att Fonder får dem. Ren JVM – inget Compose behövs.
 */
class AppBackStackTest {

    @Test
    fun `appen startar på Översikt`() {
        val stack = appBackStack()

        assertEquals(listOf<AppKey>(OversiktKey), stack.entries)
        assertEquals(OversiktKey, stack.currentTab)
        assertTrue(stack.atTopLevel)
    }

    @Test
    fun `varje flik minns sin stack när man byter flik`() {
        val stack = appBackStack()
        stack.select(InnehavKey)
        stack.push(FondDetaljKey("SHB0000442"))

        stack.select(BeslutKey)
        assertEquals(listOf(OversiktKey, BeslutKey), stack.entries)
        stack.select(InnehavKey)

        assertEquals(listOf(OversiktKey, InnehavKey, FondDetaljKey("SHB0000442")), stack.entries)
        assertFalse(stack.atTopLevel)
    }

    @Test
    fun `bakåt från en annan fliks rot går till Översikt och bakåt där lämnar appen`() {
        val stack = appBackStack()
        stack.select(HandelserKey)
        stack.push(SwitchWatchKey(3))

        assertTrue(stack.pop())
        assertEquals(listOf(OversiktKey, HandelserKey), stack.entries)
        assertTrue(stack.pop())
        assertEquals(listOf<AppKey>(OversiktKey), stack.entries)
        assertFalse(stack.pop())
    }

    @Test
    fun `att välja fliken man står på går till flikens rot`() {
        val stack = appBackStack()
        stack.select(BeslutKey)
        stack.push(FacitKey)

        stack.select(BeslutKey)

        assertEquals(listOf(OversiktKey, BeslutKey), stack.entries)
    }

    @Test
    fun `en skärm som stänger sig två gånger stänger inte skärmen under`() {
        val stack = appBackStack()
        stack.push(SettingsKey)
        stack.push(RiskProfileKey)

        stack.popIfTop(RiskProfileKey)
        stack.popIfTop(RiskProfileKey)

        assertEquals(listOf(OversiktKey, SettingsKey), stack.entries)
    }

    @Test
    fun `flikar väljs, de läggs inte på stacken`() {
        assertThrows(IllegalArgumentException::class.java) { appBackStack().push(InnehavKey) }
    }

    @Test
    fun `stackarna med Fonders nycklar överlever att sparas och återställas`() {
        val stack = appBackStack()
        stack.select(BeslutKey)
        stack.push(SwitchWatchKey(5))
        stack.push(SwitchWatchPickerKey(5))
        stack.select(InnehavKey)
        stack.push(FondDetaljKey("SHB0000442"))
        stack.push(TransactionFormKey(fundId = "SHB0000442", suggestionRecordId = 7))

        val restored = restore(stack.save())

        assertEquals(stack.entries, restored.entries)
        assertEquals(InnehavKey, restored.currentTab)
        restored.select(BeslutKey)
        assertEquals(listOf(OversiktKey, BeslutKey, SwitchWatchKey(5), SwitchWatchPickerKey(5)), restored.entries)
    }

    /** Ett läge sparat av Fonders egen stack före biblioteket (1.4.0 och äldre) har ett annat format. */
    @Test
    fun `ett läge sparat av en äldre version ger Översikt`() {
        listOf(
            "",
            "inte json",
            """{"current":"Innehav","stacks":[[{"type":"se.kodhast.fonder.ui.navigation.OversiktKey"}],[{"type":"se.kodhast.fonder.ui.navigation.InnehavKey"}],[],[]]}""",
            // Nycklar ur kodpaketet från före bytet (fonder #253).
            appBackStack().apply { select(InnehavKey) }.save().replace("se.kodhast.fonder", "se.partee71.fonder")
        ).forEach { saved ->
            val restored = restore(saved)
            assertEquals("återställt från: $saved", listOf<AppKey>(OversiktKey), restored.entries)
            assertEquals(OversiktKey, restored.currentTab)
        }
    }

    private fun restore(saved: String): AppBackStack = TabBackStack.restore(saved, TOP_LEVEL, AppKey.serializer())
}
