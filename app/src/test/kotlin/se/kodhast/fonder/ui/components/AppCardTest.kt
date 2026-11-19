package se.kodhast.fonder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.testing.captureLightAndDark
import se.kodhast.fonder.ui.theme.Error
import se.kodhast.fonder.ui.theme.Filled
import se.kodhast.fonder.ui.theme.FonderLightDepth
import se.kodhast.fonder.ui.theme.FonderTheme
import se.kodhast.fonder.ui.theme.flushCardStyle
import se.kodhast.ui.components.AppCard
import se.kodhast.ui.components.AppCardStyle
import se.kodhast.ui.components.CardSegment
import se.kodhast.ui.components.LocalAppCardStyle
import se.kodhast.ui.theme.AppDepth
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Tone

/**
 * android-ui:s [AppCard] under [FonderTheme] (UI-13) i JVM: temat ger korten Fonders kontur och luft, ett
 * kort med `onClick` är **en** klickbar nod där rubrik och belopp läses i ett svep, ett kort utan har ingen
 * egen åtgärd – och korten, de tonade och bitarna fotograferas i ljust och mörkt.
 */
@RunWith(RobolectricTestRunner::class)
class AppCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `FonderTheme ger korten djupets kant i stället för egen kontur och bibliotekets standardluft`() {
        var style: AppCardStyle? = null
        var flush: AppCardStyle? = null
        var depth: AppDepth? = null
        composeRule.setContent {
            FonderTheme(darkTheme = false) {
                style = LocalAppCardStyle.current
                flush = flushCardStyle
                depth = AppTheme.depth
            }
        }

        // Ingen egen kontur (UI-18): kortets kant och skugga kommer från djupet, tonade i nordisk blå.
        assertEquals(null, style?.border)
        assertEquals(FonderLightDepth, depth)
        // Fonder sätter ingen egen kortstil: kortets luft och avstånd är bibliotekets standard (alla appar lika).
        assertEquals(AppCardStyle(), style)
        assertEquals(0.dp, flush?.padding)
        assertEquals(0.dp, flush?.segmentGap)
        assertEquals(style?.border, flush?.border)
    }

    @Test
    fun `ett klickbart kort är en nod och ett tryck på innehållet anropar onClick`() {
        var clicks = 0
        composeRule.setContent {
            FonderTheme {
                AppCard(onClick = { clicks++ }) {
                    Text("Global Indexfond A")
                    Text("214 300 kr")
                }
            }
        }

        composeRule.onNodeWithText("Global Indexfond A", substring = true).assertHasClickAction()
        composeRule.onNodeWithText("214 300 kr", substring = true).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `ett kort utan onClick har ingen egen åtgärd`() {
        composeRule.setContent {
            FonderTheme { AppCard { Text("Portföljens värde") } }
        }

        composeRule.onNodeWithText("Portföljens värde")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun `skärmdump av varianterna`() = composeRule.captureLightAndDark("AppCard_variants") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text("Portföljens värde", style = MaterialTheme.typography.bodySmall)
                Text("412 860 kr", style = MaterialTheme.typography.headlineMedium)
            }
            AppCard(modifier = Modifier.fillMaxWidth(), tone = Tone.Filled, onClick = {}) {
                Text("Tillväxtmarknad Bred", style = MaterialTheme.typography.titleSmall)
                Text("Avgift 372 kr per år", style = MaterialTheme.typography.bodySmall)
            }
            AppCard(modifier = Modifier.fillMaxWidth(), tone = Tone.Error, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                Text("Farozon", style = MaterialTheme.typography.titleSmall)
                Text("Det här går inte att ångra.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    @Test
    fun `bitar av ett kort utan kontur ligger kant i kant – tre bitar är tre bitars höjd`() {
        composeRule.setContent {
            FonderTheme {
                Column(modifier = Modifier.testTag("grupp")) {
                    repeat(3) { index ->
                        CardSegment(top = index == 0, bottom = index == 2, modifier = Modifier.height(50.dp)) { Text("Rad $index") }
                    }
                }
            }
        }

        // Utan kontur dras bitarna inte upp: djupets kant ritas runt hela kortet, inte i skarvarna.
        composeRule.onNodeWithTag("grupp").assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun `skärmdump av ett kort av bitar`() = composeRule.captureLightAndDark("AppCard_grouped") {
        Column {
            listOf("Sverige Småbolag", "Global Indexfond A", "Räntefond Kort").forEachIndexed { index, name ->
                CardSegment(top = index == 0, bottom = index == 2) { Text(name, style = MaterialTheme.typography.titleSmall) }
            }
        }
    }
}
