package se.kodhast.fonder.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.domain.model.Fund

/**
 * Fondsöks val i valläget går till skärmen under (HEM-10, ANA-13): det tas emot en gång och kvitteras, och
 * ett val som ännu inte tagits emot överlever en rotation. Förut sparades valet av valskärmens egen
 * ViewModel, som rensades när skärmen stängdes – sparningen kunde avbrytas och felet syntes aldrig.
 */
@RunWith(RobolectricTestRunner::class)
class FundPickTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val fund = Fund(fundId = "SHB1", name = "Global Index", isin = "SE0011527613")

    @Test
    fun `ett val tas emot en gang och kvitteras`() {
        val pending = mutableStateOf<FundPick?>(null)
        val received = mutableListOf<Fund>()
        composeRule.setContent {
            PickedFundEffect(
                fund = (pending.value as? FundPick.Benchmark)?.fund,
                onPick = { received += it },
                onAcknowledge = { pending.value = null }
            )
        }

        composeRule.runOnIdle { pending.value = FundPick.Benchmark(fund) }
        composeRule.waitForIdle()

        assertEquals(listOf(fund), received)
        assertNull(pending.value)
    }

    @Test
    fun `ett alternativ till en annan bevakning tas inte emot`() {
        val pending = mutableStateOf<FundPick?>(FundPick.Candidate(watchId = 2, fund = fund))
        val received = mutableListOf<Fund>()
        composeRule.setContent {
            PickedFundEffect(
                fund = (pending.value as? FundPick.Candidate)?.takeIf { it.watchId == 1L }?.fund,
                onPick = { received += it },
                onAcknowledge = { pending.value = null }
            )
        }
        composeRule.waitForIdle()

        assertEquals(emptyList<Fund>(), received)
        assertEquals(FundPick.Candidate(watchId = 2, fund = fund), pending.value)
    }

    @Test
    fun `ett vantande val overlever en rotation`() {
        val restorationTester = StateRestorationTester(composeRule)
        lateinit var pending: MutableState<FundPick?>
        restorationTester.setContent { pending = rememberFundPick() }

        composeRule.runOnIdle { pending.value = FundPick.Candidate(watchId = 7, fund = fund) }
        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.runOnIdle { assertEquals(FundPick.Candidate(watchId = 7, fund = fund), pending.value) }
    }

    @Test
    fun `valet nar Installningars ViewModel fast valskarmen stangs direkt`() {
        val backStack = appBackStack().apply {
            push(SettingsKey)
            push(BenchmarkPickerKey)
        }
        lateinit var pending: MutableState<FundPick?>
        lateinit var settings: OwnerViewModel
        composeRule.setContent {
            pending = rememberFundPick()
            FonderNavHost(
                backStack = backStack,
                entryProvider = { key ->
                    NavEntry(key) {
                        when (key) {
                            SettingsKey -> {
                                settings = viewModel()
                                PickedFundEffect(
                                    fund = (pending.value as? FundPick.Benchmark)?.fund,
                                    onPick = { settings.received += it },
                                    onAcknowledge = { pending.value = null }
                                )
                                Text("inställningar")
                            }

                            BenchmarkPickerKey -> Text(
                                "välj",
                                Modifier.clickable {
                                    pending.value = FundPick.Benchmark(fund)
                                    backStack.popIfTop(key)
                                }
                            )

                            else -> Text(key.toString())
                        }
                    }
                }
            )
        }

        composeRule.onNodeWithText("välj").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("inställningar").assertExists()
        assertEquals(listOf(fund), settings.received)
        assertNull(pending.value)
    }

    /** Skärmen under valskärmen: dess ViewModel ligger kvar och tar emot valet. */
    class OwnerViewModel : ViewModel() {
        val received = mutableListOf<Fund>()
    }
}
