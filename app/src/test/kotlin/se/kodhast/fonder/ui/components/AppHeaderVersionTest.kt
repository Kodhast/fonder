package se.kodhast.fonder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.R
import se.kodhast.fonder.testing.captureLightAndDark
import se.kodhast.fonder.ui.theme.FonderTheme
import se.kodhast.ui.components.APP_HEADER_VERSION_TAG
import se.kodhast.ui.components.AppHeader
import se.kodhast.ui.components.AppIdentity
import se.kodhast.ui.components.LocalAppIdentity

/** Versionen i sidhuvudet (NAV-7): temats `appVersion` står efter appnamnet; utan den ritas ingen version. */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class AppHeaderVersionTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(appVersion: String?) = rule.setContent {
        FonderTheme(appVersion = appVersion) { AppHeader("Översikt", TopAppBarDefaults.exitUntilCollapsedScrollBehavior()) }
    }

    private fun version() = rule.onNodeWithTag(APP_HEADER_VERSION_TAG, useUnmergedTree = true)

    @Test
    fun `temats appVersion står efter appnamnet`() {
        show("1.9.6")
        version().assertIsDisplayed()
        version().assertTextEquals("1.9.6")
    }

    @Test
    fun `utan appVersion ritas ingen version`() {
        show(null)
        version().assertDoesNotExist()
    }

    /** Identiteten sätts direkt så att bilden bär ett påhittat nummer, inte appens riktiga. */
    @Test
    fun `skärmdump - versionen i sidhuvudet`() = rule.captureLightAndDark("AppHeader_vila_version") {
        CompositionLocalProvider(LocalAppIdentity provides AppIdentity(R.drawable.ic_app_header, "Fonder", "1.9.6")) {
            Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                AppHeader("Översikt", TopAppBarDefaults.exitUntilCollapsedScrollBehavior())
            }
        }
    }
}
