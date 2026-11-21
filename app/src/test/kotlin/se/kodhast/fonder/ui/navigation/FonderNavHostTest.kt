package se.kodhast.fonder.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.testing.captureLightAndDark
import se.kodhast.fonder.ui.theme.FonderTheme
import se.kodhast.ui.common.DetailUiState
import se.kodhast.ui.components.APP_HEADER_TAG
import se.kodhast.ui.components.AccountMenuAvatar
import se.kodhast.ui.components.EntityDetailScreen
import se.kodhast.ui.components.SettingsPage

/**
 * Fonders navigationsram (NAV-7, NAV-8, UI-8) i JVM med Robolectric – android-ui:s `AppNavHost` med Fonders
 * flikrad, plusknapp och meny, men enkla ersättare för skärmarna
 * så att testet inte behöver Hilt: fyra flikar med Översikt som start och Kodhäst-sidhuvudet, plusknappens
 * meny med de fyra valen i ordning, som öppnar sina skärmar, bakåt som stänger menyn, Inställningar bakom
 * avatarmenyn, flikraden och plusmenyn borta på en djupare skärm, underskärmarnas topprad med
 * tillbakapil, ramskärmarna utan, bakåt från en annan flik till Översikt, att en flik behåller sitt tillstånd
 * vid flikbyte, att stacken återställs efter rotation och sidhuvudets prickar under en kursuppdatering (NAV-6).
 */
@RunWith(RobolectricTestRunner::class)
class FonderNavHostTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val backStack = appBackStack()

    /** Ersättarskärmens text – flikarnas och skärmarnas gamla ruttnamn, så skärmdumpen är oförändrad. */
    private fun name(key: AppKey): String = when (key) {
        OversiktKey -> "hem"
        InnehavKey -> "portfolj"
        BeslutKey -> "beslut"
        HandelserKey -> "transaktioner"
        SettingsKey -> "settings"
        FacitKey -> "facit"
        is TransactionFormKey -> "transaction-form"
        ImportOrdersKey -> "import-orders"
        ImportHoldingsKey -> "import-holdings"
        FundSearchKey -> "fund-search"
        is FondDetaljKey -> "fond/${key.fundId}"
        else -> key.toString()
    }

    /**
     * Ersättarskärmar: Innehav öppnar en fond, och varje skärm räknar tryck i sparat tillstånd och
     * i sin ViewModel.
     */
    private fun stubEntry(key: AppKey): NavEntry<AppKey> = NavEntry(key) {
        val viewModel: StubViewModel = viewModel()
        viewModels[key] = viewModel
        var taps by rememberSaveable { mutableIntStateOf(0) }
        val body: @Composable () -> Unit = {
            Column {
                Text("skärm:${name(key)}", Modifier.clickable { taps++ })
                Text("tryck:$taps")
                if (key == InnehavKey) {
                    Text("öppna fond", Modifier.clickable { backStack.push(FondDetaljKey(FUND_ID)) })
                }
            }
        }
        Frame(key, body)
    }

    /** Ersättarens ram: samma topprad eller sidhuvud som den riktiga skärmen ritar. */
    @Composable
    private fun Frame(key: AppKey, body: @Composable () -> Unit) {
        val content = remember(body) { movableContentOf(body) }
        when {
            tabTitle(key) != null -> TabFrame(tabTitle(key)!!, content)

            // Inställningar står på bibliotekets SettingsPage och ritar sin egen topprad (#266 F4).
            key == SettingsKey -> SettingsPage("Inställningar", onBack = { backStack.popIfTop(key) }) { content() }

            // Facit, importerna och Pågående byte står på bibliotekets list- och detaljramar och ritar sin
            // egen lilla topprad med tillbakapil (#266 F6) – ersättarna gör likadant.
            subTitle(key) != null -> EntityDetailScreen(
                state = DetailUiState.Content(Unit),
                header = null,
                onBack = { backStack.popIfTop(key) },
                title = subTitle(key)
            ) { content() }

            else -> content()
        }
    }

    /**
     * Översikt, Innehav och Beslut står på bibliotekets detalj- eller listram i flikläge och ritar sitt eget
     * Kodhäst-sidhuvud (#266 F3) – ersättarna gör likadant, så att skalets del (identiteten och prickarna)
     * syns. Händelser och underskärmarna ritas som de är.
     */
    @Composable
    private fun TabFrame(title: String, content: @Composable () -> Unit) {
        EntityDetailScreen(state = DetailUiState.Content(Unit), header = null, onBack = null, title = title) { content() }
    }

    private fun subTitle(key: AppKey): String? = when (key) {
        FacitKey -> "Facit"
        ImportHoldingsKey -> "Importera innehav"
        ImportOrdersKey -> "Importera avräkningsnotor"
        is SwitchWatchKey -> "Pågående byte"
        else -> null
    }

    private fun tabTitle(key: AppKey): String? = when (key) {
        OversiktKey -> "Översikt"
        InnehavKey -> "Innehav"
        BeslutKey -> "Beslut"
        else -> null
    }

    private val viewModels = mutableMapOf<AppKey, StubViewModel>()

    private fun setShell(beslutBadgeCount: Int = 0) = composeRule.setContent {
        FonderTheme {
            FonderNavHost(
                backStack = backStack,
                entryProvider = ::stubEntry,
                beslutBadgeCount = beslutBadgeCount,
                account = {
                    AccountMenuAvatar(
                        account = null,
                        appName = "Fonder",
                        onOpenSettings = { backStack.push(SettingsKey) },
                        onOpenAbout = { backStack.push(AboutKey) },
                        onSignIn = {},
                        onSignOut = {}
                    )
                }
            )
        }
    }

    private fun assertOnScreen(name: String) = composeRule.onNodeWithText("skärm:$name").assertIsDisplayed()

    private fun pressBack() {
        // Bakåthanterarna följer kompositionen – vänta in den innan bakåt trycks.
        composeRule.waitForIdle()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun assertMenu(expanded: Boolean) {
        composeRule.onNodeWithContentDescription("Lägg till")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, if (expanded) "Utfälld" else "Hopfälld"))
        val first = composeRule.onAllNodesWithText("Ny transaktion")
        if (expanded) first.assertCountEquals(1) else first.assertCountEquals(0)
    }

    private fun assertTabChrome(visible: Boolean) {
        if (visible) {
            composeRule.onNodeWithContentDescription("Händelser").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Lägg till").assertIsDisplayed()
        } else {
            composeRule.onNodeWithContentDescription("Händelser").assertDoesNotExist()
            composeRule.onNodeWithContentDescription("Lägg till").assertDoesNotExist()
        }
    }

    @Test
    fun `fyra flikar med Översikt som start och plusmenyn på varje flik`() {
        setShell()

        assertOnScreen("hem")
        composeRule.onNodeWithContentDescription("Översikt").assertIsSelected()
        listOf(
            "Innehav" to "portfolj",
            "Beslut" to "beslut",
            "Händelser" to "transaktioner",
            "Översikt" to "hem"
        ).forEach { (label, route) ->
            composeRule.onNodeWithContentDescription(label).performClick()
            assertOnScreen(route)
            composeRule.onNodeWithContentDescription(label).assertIsSelected()
            // Flikens rubrik i Kodhäst-sidhuvudet, ingen bakåtpil – Händelser står på listramen och ritar sitt
            // eget. (Den valda fliken i flikraden visar också namnet, men är ingen rubrik.)
            if (backStack.currentTab == HandelserKey) {
                composeRule.onAllNodes(hasText(label) and isHeading()).assertCountEquals(0)
            } else {
                composeRule.onNode(hasText(label) and isHeading()).assertIsDisplayed()
                composeRule.onNodeWithTag(APP_HEADER_TAG).assertIsDisplayed()
            }
            composeRule.onNodeWithContentDescription("Tillbaka").assertDoesNotExist()
            assertTabChrome(visible = true)
        }
    }

    @Test
    fun `plusknappen fäller ut en meny med de fyra valen i ordning`() {
        setShell()
        assertMenu(expanded = false)

        composeRule.onNodeWithContentDescription("Lägg till").performClick()

        assertMenu(expanded = true)
        val tops = listOf("Ny transaktion", "Importera avräkningsnota", "Importera innehav", "Bevaka ny fond")
            .map { composeRule.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(tops.sorted(), tops)
    }

    @Test
    fun `plusmenyn öppnar sina skärmar och döljs med flikraden på djupare skärmar`() {
        setShell()

        listOf(
            "Ny transaktion" to "transaction-form",
            "Importera avräkningsnota" to "import-orders",
            "Importera innehav" to "import-holdings",
            "Bevaka ny fond" to "fund-search"
        ).forEach { (label, route) ->
            composeRule.onNodeWithContentDescription("Lägg till").performClick()
            composeRule.onNodeWithText(label).performClick()

            assertOnScreen(route)
            assertTabChrome(visible = false)
            // Transaktionsformuläret står på en ram utan navigationens topprad – systemets bakåt gäller alla.
            pressBack()
            assertOnScreen("hem")
            assertTabChrome(visible = true)
        }
    }

    @Test
    fun `plusmenyn stängs när man lämnar fliken och är stängd när man kommer tillbaka`() {
        setShell()
        composeRule.onNodeWithContentDescription("Lägg till").performClick()
        assertMenu(expanded = true)

        composeRule.runOnUiThread { backStack.push(SettingsKey) }
        assertOnScreen("settings")
        composeRule.onNodeWithContentDescription("Tillbaka").performClick()

        assertOnScreen("hem")
        assertMenu(expanded = false)
    }

    @Test
    fun `avatarmenyn står på de tre flikarna med sidhuvud och öppnar Inställningar (NAV-7)`() {
        setShell()
        listOf("Innehav", "Beslut", "Översikt").forEach { tab ->
            composeRule.onNodeWithContentDescription(tab).performClick()
            composeRule.onNodeWithContentDescription("Konto och inställningar").assertIsDisplayed()
        }

        composeRule.onNodeWithContentDescription("Konto och inställningar").performClick()
        composeRule.onNodeWithText("Logga in med Google").assertIsDisplayed()
        composeRule.onNodeWithText("Inställningar").performClick()

        assertOnScreen("settings")
    }

    @Test
    fun `Inställningar är en underskärm med bakåtpil och utan flikrad`() {
        setShell()

        composeRule.runOnUiThread { backStack.push(SettingsKey) }
        composeRule.waitForIdle()

        assertOnScreen("settings")
        composeRule.onNodeWithText("Inställningar").assertIsDisplayed()
        assertTabChrome(visible = false)
        composeRule.onNodeWithContentDescription("Tillbaka").performClick()
        assertOnScreen("hem")
    }

    @Test
    fun `Händelser har ingen länk till Sålda fonder – säljen står i listan`() {
        // Sedan 0.62.0 (SLD-1, NAV-5): Sålda fonder är en del av Händelser, inte en egen skärm.
        setShell()

        composeRule.onNodeWithContentDescription("Händelser").performClick()

        assertOnScreen("transaktioner")
        assertTabChrome(visible = true)
        composeRule.onNodeWithText("Sålda fonder").assertDoesNotExist()
    }

    @Test
    fun `Beslut bär kopparmärket med antalet och läser upp det`() {
        setShell(beslutBadgeCount = 3)

        composeRule.onNodeWithContentDescription("Beslut, 3 nya").assertIsDisplayed()
    }

    @Test
    fun `utan något att besluta ritas inget märke`() {
        setShell(beslutBadgeCount = 0)

        composeRule.onNodeWithContentDescription("Beslut").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Beslut, 0 nya").assertDoesNotExist()
    }

    @Test
    fun `Facit är en skärm i stacken under Beslut med bakåtpil`() {
        setShell()
        composeRule.onNodeWithContentDescription("Beslut").performClick()

        composeRule.runOnUiThread { backStack.push(FacitKey) }

        assertOnScreen("facit")
        composeRule.onNodeWithText("Facit").assertIsDisplayed()
        assertTabChrome(visible = false)
        composeRule.onNodeWithContentDescription("Tillbaka").performClick()
        assertOnScreen("beslut")
        assertTabChrome(visible = true)
    }

    @Test
    fun `Fonddetalj öppnas från Innehav och bakåt leder tillbaka till Innehav`() {
        setShell()
        composeRule.onNodeWithContentDescription("Innehav").performClick()

        composeRule.onNodeWithText("öppna fond").performClick()

        assertOnScreen("fond/$FUND_ID")
        // Fonddetalj står på detaljramen: namnet i ramens topprad, navigationen ritar ingen (NAV-2).
        composeRule.onAllNodesWithText("Fond").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Tillbaka").assertCountEquals(0)
        assertTabChrome(visible = false)
        pressBack()
        assertOnScreen("portfolj")
        composeRule.onNodeWithContentDescription("Innehav").assertIsSelected()
    }

    @Test
    fun `bakåt från en annan flik går till Översikt`() {
        setShell()

        composeRule.onNodeWithContentDescription("Innehav").performClick()
        composeRule.onNodeWithContentDescription("Händelser").performClick()
        assertOnScreen("transaktioner")
        composeRule.onNodeWithContentDescription("Händelser").assertIsSelected()

        pressBack()

        assertOnScreen("hem")
        composeRule.onNodeWithContentDescription("Översikt").assertIsSelected()
    }

    /**
     * Flikrotens sparade tillstånd (rullning, här trycken) behålls vid flikbyte. ViewModeln behålls av att
     * flikarnas ViewModels hämtas från aktiviteten i `fonderEntries`, inte av navigationsramen.
     */
    @Test
    fun `en flik behåller sitt tillstånd när man byter flik och tillbaka`() {
        setShell()
        composeRule.onNodeWithContentDescription("Innehav").performClick()
        composeRule.onNodeWithText("skärm:portfolj").performClick()
        composeRule.onNodeWithText("tryck:1").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Beslut").performClick()
        assertOnScreen("beslut")
        composeRule.onNodeWithContentDescription("Innehav").performClick()

        assertOnScreen("portfolj")
        composeRule.onNodeWithText("tryck:1").assertIsDisplayed()
    }

    @Test
    fun `en underskärm som stängs börjar om nästa gång`() {
        setShell()
        composeRule.runOnUiThread { backStack.push(SettingsKey) }
        composeRule.onNodeWithText("skärm:settings").performClick()
        composeRule.onNodeWithText("tryck:1").assertIsDisplayed()
        val settingsViewModel = viewModels.getValue(SettingsKey)

        pressBack()
        assertOnScreen("hem")
        composeRule.runOnUiThread { backStack.push(SettingsKey) }

        composeRule.onNodeWithText("tryck:0").assertIsDisplayed()
        assertNotSame(settingsViewModel, viewModels.getValue(SettingsKey))
        assertEquals(true, settingsViewModel.cleared)
    }

    @Test
    fun `bakåt stänger en öppen plusmeny i stället för att lämna fliken`() {
        setShell()
        composeRule.onNodeWithContentDescription("Innehav").performClick()
        composeRule.onNodeWithContentDescription("Lägg till").performClick()
        assertMenu(expanded = true)

        // Menyn är ett eget fönster: systemets bakåt går dit, inte till aktivitetens bakåthanterare.
        composeRule.waitForIdle()
        Espresso.pressBack()

        assertMenu(expanded = false)
        assertOnScreen("portfolj")
    }

    @Test
    fun `stacken överlever att skärmen återskapas`() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            FonderTheme {
                FonderNavHost(backStack = rememberAppBackStack(), entryProvider = ::stubEntry)
            }
        }
        composeRule.onNodeWithContentDescription("Innehav").performClick()

        restoration.emulateSavedInstanceStateRestore()

        assertOnScreen("portfolj")
        composeRule.onNodeWithContentDescription("Innehav").assertIsSelected()
        assertTabChrome(visible = true)
        pressBack()
        assertOnScreen("hem")
    }

    @Test
    fun `navigationen ritar ingen topprad – underskärmarna har sin ram med rubrik och tillbakapil`() {
        setShell()
        listOf(
            ImportHoldingsKey to "Importera innehav",
            ImportOrdersKey to "Importera avräkningsnotor",
            SwitchWatchKey(3) to "Pågående byte"
        ).forEach { (key, title) ->
            composeRule.runOnUiThread { backStack.push(key) }
            composeRule.waitForIdle()
            composeRule.onNode(hasText(title) and isHeading()).assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Tillbaka").performClick()
            assertOnScreen("hem")
        }
        listOf(RiskProfileKey, BenchmarkPickerKey, SwitchWatchPickerKey(3), ComponentGalleryKey).forEach { key ->
            composeRule.runOnUiThread { backStack.push(key) }
            composeRule.waitForIdle()
            composeRule.onAllNodesWithContentDescription("Tillbaka").assertCountEquals(0)
            pressBack()
            assertOnScreen("hem")
        }
    }

    @Test
    fun `sidhuvudets prickar säger att kurser uppdateras medan bakgrundsjobbet kör`() {
        // Prickarna hoppar oändligt – klockan stegas för hand i stället för att vänta på vila.
        composeRule.mainClock.autoAdvance = false
        var running by mutableStateOf(true)
        composeRule.setContent {
            FonderTheme { FonderNavHost(backStack = backStack, entryProvider = ::stubEntryWithoutTaps, isWorkerRunning = running) }
        }
        composeRule.mainClock.advanceTimeBy(TRANSITION_MS)
        composeRule.onAllNodesWithContentDescription(WORKER_RUNNING).assertCountEquals(1)

        running = false
        composeRule.mainClock.advanceTimeBy(TRANSITION_MS)

        composeRule.onAllNodesWithContentDescription(WORKER_RUNNING).assertCountEquals(0)
    }

    @Test
    fun `skärmdump av skalet på Översikt`() {
        composeRule.captureLightAndDark("AppNavigation_oversikt") {
            FonderNavHost(backStack = rememberAppBackStack(), entryProvider = ::stubEntryWithoutTaps)
        }
    }

    /** Skärmdumpens ersättare – bara skärmens namn, som före Navigation 3. */
    private fun stubEntryWithoutTaps(key: AppKey): NavEntry<AppKey> = NavEntry(key) {
        val body: @Composable () -> Unit = { Text("skärm:${name(key)}") }
        Frame(key, body)
    }

    class StubViewModel : ViewModel() {
        var cleared = false
            private set

        override fun onCleared() {
            cleared = true
        }
    }

    private companion object {
        const val FUND_ID = "SHB0000442"
        const val WORKER_RUNNING = "Uppdaterar kurser i bakgrunden"

        /** Längre än skärmbytets övergång (AppTransitions), så att bara målskärmen står kvar. */
        const val TRANSITION_MS = 2_000L
    }
}
