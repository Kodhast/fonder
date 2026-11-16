package se.kodhast.fonder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.ui.theme.FonderTheme

/**
 * [CenteredState] (NFR-6, UI-5) i JVM: innehållet står mitt på ytan, och när det inte ryms går ytan att
 * rulla så att det nedersta (t.ex. "Försök igen") nås.
 */
@RunWith(RobolectricTestRunner::class)
class CenteredStateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `innehållet står mitt på ytan`() {
        composeRule.setContent {
            FonderTheme {
                Box(Modifier.size(300.dp)) {
                    CenteredState { Box(Modifier.size(100.dp).testTag("innehåll")) }
                }
            }
        }

        val content = composeRule.onNodeWithTag("innehåll").getUnclippedBoundsInRoot()
        assertEquals(100.dp, content.top)
        assertEquals(100.dp, content.left)
    }

    @Test
    fun `innehåll som inte ryms går att rulla fram`() {
        composeRule.setContent {
            FonderTheme {
                Box(Modifier.size(200.dp)) {
                    CenteredState {
                        Box(Modifier.height(400.dp))
                        Text("Försök igen")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Försök igen").performScrollTo().assertIsDisplayed()
    }
}
