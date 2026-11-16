package se.kodhast.fonder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.R
import se.kodhast.fonder.testing.captureLightAndDark
import se.kodhast.fonder.ui.theme.FonderTheme
import se.kodhast.fonder.ui.theme.NeutralPrimary
import se.kodhast.ui.components.AppIconButton
import se.kodhast.ui.components.AppTopBar
import se.kodhast.ui.components.IconButtonVariant
import se.kodhast.ui.components.LocalTopBarBackStyle
import se.kodhast.ui.components.TopBarBackStyle
import se.kodhast.ui.components.TopBarSize
import se.kodhast.ui.theme.Tone

/**
 * Fonders [AppTabHeading] och android-ui:s lilla [AppTopBar] under [FonderTheme] (UI-8, NAV-7, UI-14) i
 * JVM: toppraden har en rubrik som läses som rubrik och bibliotekets tonade bakåtpil som läses
 * "Tillbaka" (temat sätter ingen egen pilstil), fliken en rubrik som skärmläsaren läser som rubrik och
 * sina åtgärder till höger.
 */
@RunWith(RobolectricTestRunner::class)
class AppTabHeadingTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `toppraden visar rubriken och bakåtpilen går tillbaka`() {
        var backs = 0
        var backStyle: TopBarBackStyle? = null
        composeRule.setContent {
            FonderTheme {
                backStyle = LocalTopBarBackStyle.current
                AppTopBar(title = "Pågående byte", size = TopBarSize.Small, onBack = { backs++ })
            }
        }

        assertEquals(TopBarBackStyle(), backStyle)
        composeRule.onNodeWithText("Pågående byte").assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithContentDescription("Tillbaka").assertTouchHeightIsEqualTo(48.dp).performClick()

        assertEquals(1, backs)
    }

    @Test
    fun `flikens rubrik läses som rubrik och har sina åtgärder`() {
        var clicks = 0
        composeRule.setContent {
            FonderTheme {
                AppTabHeading(title = "Översikt") {
                    AppIconButton(
                        icon = R.drawable.ic_settings,
                        contentDescription = "Inställningar",
                        onClick = { clicks++ },
                        variant = IconButtonVariant.Tonal,
                        tone = Tone.NeutralPrimary
                    )
                }
            }
        }

        composeRule.onNodeWithText("Översikt").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithContentDescription("Inställningar").assertTouchHeightIsEqualTo(48.dp).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `skärmdump ljust och mörkt`() = composeRule.captureLightAndDark("AppTopBar_variants") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppTopBar(title = "Fonddetalj", size = TopBarSize.Small, onBack = {})
            AppTabHeading(title = "Översikt") {
                AppIconButton(
                    icon = R.drawable.ic_settings,
                    contentDescription = "Inställningar",
                    onClick = {},
                    variant = IconButtonVariant.Tonal,
                    tone = Tone.NeutralPrimary
                )
            }
        }
    }
}
