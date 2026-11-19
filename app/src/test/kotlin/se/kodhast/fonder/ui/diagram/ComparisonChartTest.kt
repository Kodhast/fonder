package se.kodhast.fonder.ui.diagram

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.time.LocalDate
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.kodhast.fonder.testing.captureLightAndDark
import se.kodhast.fonder.testing.isDrawn
import se.kodhast.fonder.ui.theme.FonderTheme

/**
 * Det delade jämförelsediagrammet [ComparisonChart] (ANA-11, ANA-12, #266 F6) i JVM med Robolectric: hämtningen
 * och den saknade jämförelsen säger det i text och ritar inget tomt diagram (UI-19, ANA-4), och ett diagram vars
 * alternativ inte landat säger att hämtningen pågår under kurvan. Skärmdump ljust + mörkt av diagrammet med
 * texten under. Påhittade kurser.
 */
@RunWith(RobolectricTestRunner::class)
class ComparisonChartTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val end = LocalDate.of(2026, 10, 9).toEpochDay()

    private fun series(base: Double, slope: Double) = (0L until 30L).map { day ->
        (end - 29 + day) to base + slope * day + 4 * sin(day / 3.0)
    }

    private val unavailable = "Ingen jämförelse visas."
    private val loadingText = "Hämtar kurshistorik för jämförelsen …"

    private fun show(available: Boolean, loading: Boolean) = composeRule.setContent {
        FonderTheme {
            ComparisonChart(
                points = series(300.0, 0.8),
                comparisonSeries = listOf(ChartSeries("Global Indexfond A", series(120.0, -0.2))),
                available = available,
                unavailableText = unavailable,
                loading = loading
            )
        }
    }

    @Test
    fun `utan jämförelse medan den hämtas står hämtningen i text, inte den saknade jämförelsen`() {
        show(available = false, loading = true)
        composeRule.onNodeWithText(loadingText).assertIsDisplayed()
        composeRule.onNodeWithText(unavailable).assertDoesNotExist()
    }

    @Test
    fun `utan jämförelse när inget hämtas säger texten varför`() {
        show(available = false, loading = false)
        composeRule.onNodeWithText(unavailable).assertIsDisplayed()
        composeRule.onNodeWithText(loadingText).assertDoesNotExist()
    }

    @Test
    fun `ett färdigt diagram har ingen hämtningstext`() {
        show(available = true, loading = false)
        composeRule.onNodeWithText(loadingText).assertDoesNotExist()
        composeRule.onNodeWithText(unavailable).assertDoesNotExist()
    }

    @Test
    fun `skärmdump av diagrammet medan ett alternativ hämtas`() = composeRule.captureLightAndDark("ComparisonChart_loading", ready = { isDrawn() }) {
        ComparisonChart(
            points = series(300.0, 0.8),
            comparisonSeries = listOf(ChartSeries("Global Indexfond A", series(120.0, -0.2))),
            available = true,
            unavailableText = unavailable,
            loading = true,
            primaryLabel = "Tillväxtmarknad Bred"
        )
    }
}
