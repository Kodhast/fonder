package se.kodhast.fonder.ui.imports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.testing.captureLightAndDark
import se.kodhast.fonder.ui.theme.FonderTheme

/**
 * Importradens kryss ([ImportRowHeader], UI-13) i JVM: android-ui:s `CheckRow`, där fondnamn och
 * undertext är **en** nod med rollen kryssruta och minst 48 dp hög, ett tryck växlar värdet (UI-3),
 * och kryss betyder "ta med" – titeln stryks inte över.
 */
@RunWith(RobolectricTestRunner::class)
class ImportRowHeaderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var included: Boolean? = null

    private fun setHeader(checked: Boolean) = composeRule.setContent {
        FonderTheme { Column { header(checked) { included = it } } }
    }

    @Composable
    private fun ColumnScope.header(checked: Boolean, onChange: (Boolean) -> Unit = {}) = ImportRowHeader(
        included = checked,
        onIncludedChange = onChange,
        fundName = "Global Indexfond A",
        supportingText = "ISIN SE0000000001",
        detail = "120,5 andelar",
        catalogFunds = emptyList(),
        matchedFund = null,
        matchConfidence = null,
        priceFetchFailed = false,
        onFundOverride = {}
    )

    @Test
    fun `kryssraden är en nod med rollen kryssruta och ett tryck växlar värdet`() {
        setHeader(checked = false)

        composeRule.onNodeWithText("Global Indexfond A")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertHeightIsAtLeast(48.dp)
            .assertIsOff()
        // Undertexten hör till samma nod: ingen lös text bredvid krysset.
        composeRule.onAllNodesWithText("ISIN SE0000000001").fetchSemanticsNodes().single()
        composeRule.onNodeWithText("ISIN SE0000000001").performClick()

        assertEquals(true, included)
    }

    @Test
    fun `ett markerat värde läses som på`() {
        setHeader(checked = true)

        composeRule.onNodeWithText("Global Indexfond A").assertIsOn()
    }

    @Test
    fun `skärmdump ikryssad och inte`() = composeRule.captureLightAndDark("ImportRowHeader_default") {
        Column {
            header(checked = true)
            header(checked = false)
        }
    }
}
