package se.kodhast.fonder.testing

import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.ui.components.EmptyContent
import se.kodhast.ui.components.EntityListScreen
import se.kodhast.ui.components.ItemRow
import se.kodhast.ui.components.R as UiR
import se.kodhast.ui.testing.runListScreenContract

/**
 * [contracts] fungerar med bibliotekets ramar i Fonders tema: felet går att försöka igen, och
 * listkontraktet går igenom för en `EntityListScreen` utan lägg till (som flikarna, där plusmenyn
 * sitter på samma plats).
 */
@RunWith(RobolectricTestRunner::class)
class ContractsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `felet i kontrakten går att försöka igen`() {
        assertTrue(contracts.errors.map(contracts.retryableFailure).retryable)
    }

    @Test
    fun `listramen i Fonders tema uppfyller listkontraktet`() {
        val empty = EmptyContent(UiR.drawable.ic_box, "Inga fonder hittades", "Prova ett annat namn.")
        rule.runListScreenContract(contracts, "Global Indexfond A", "Global Indexfond A", empty.title, addLabel = null) { state, _, onRetry ->
            EntityListScreen("Fondsök", state, empty, add = null, key = { it }, onRetry = onRetry) { ItemRow(it) }
        }
    }
}
