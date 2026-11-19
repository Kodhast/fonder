package se.kodhast.fonder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import se.kodhast.fonder.domain.model.Fund

/**
 * En fond som valts i Fondsöks valläge och ska tas emot av skärmen under: referensfonden i Inställningar
 * (HEM-10) eller ett nytt alternativ i Pågående byte (ANA-13).
 *
 * Valskärmen stänger sig direkt efter valet. Därför får den inte själv spara valet i en egen ViewModel –
 * den rensas när posten tas bort, och en sparning som väntar på en ISIN-uppslagning avbryts då, och ett fel
 * hamnar på en ViewModel som ingen skärm visar. Valet lämnas i stället hit och tas emot av skärmen under,
 * vars ViewModel ligger kvar och visar felet.
 */
@Serializable
sealed interface FundPick {
    val fund: Fund

    /** Referensfonden för indexjämförelsen (HEM-10), till Inställningar. */
    @Serializable
    data class Benchmark(override val fund: Fund) : FundPick

    /** Ett alternativ till bevakningen [watchId] (ANA-13), till Pågående byte. */
    @Serializable
    data class Candidate(val watchId: Long, override val fund: Fund) : FundPick
}

private val fundPickSaver: Saver<MutableState<FundPick?>, String> = Saver(
    save = { state -> state.value?.let { Json.encodeToString(FundPick.serializer(), it) }.orEmpty() },
    restore = { saved -> mutableStateOf(saved.takeIf { it.isNotEmpty() }?.let { Json.decodeFromString(FundPick.serializer(), it) }) }
)

/** Ett väntande val, sparat över rotation och processdöd tills skärmen under tagit emot det. */
@Composable
fun rememberFundPick(): MutableState<FundPick?> = rememberSaveable(saver = fundPickSaver) { mutableStateOf(null) }

/**
 * Tar emot ett väntande val en gång: [onPick] med fonden, sedan [onAcknowledge] som kvitterar det. `null` gör inget.
 */
@Composable
fun PickedFundEffect(fund: Fund?, onPick: (Fund) -> Unit, onAcknowledge: () -> Unit) {
    val pick by rememberUpdatedState(onPick)
    val handled by rememberUpdatedState(onAcknowledge)
    LaunchedEffect(fund) {
        if (fund != null) {
            pick(fund)
            handled()
        }
    }
}
