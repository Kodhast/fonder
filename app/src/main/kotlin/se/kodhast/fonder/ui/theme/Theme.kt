package se.kodhast.fonder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import se.kodhast.fonder.R
import se.kodhast.ui.components.ActionMenuStyle
import se.kodhast.ui.components.AppBrandStyle
import se.kodhast.ui.components.AppCardStyle
import se.kodhast.ui.components.AppIdentity
import se.kodhast.ui.components.LocalAppBrandStyle
import se.kodhast.ui.components.LocalAppIdentity
import se.kodhast.ui.components.ToolbarStyle
import se.kodhast.ui.theme.AppTheme
import se.kodhast.ui.theme.Tone

/**
 * Fonders egna färger för det läge appen **faktiskt** applicerade, till skillnad från systemets
 * ([isSystemInDarkTheme]). [ReturnColors] läser dem härifrån och inte ur systemläget – annars
 * följde vinst/förlust telefonens läge medan alla ytor följde användarens val i Inställningar
 * (ljusa lågkontrastfärger på vit bakgrund, och tvärtom).
 */
private val LocalFonderColors = staticCompositionLocalOf { FonderLightColors }

/**
 * Fonders tema: android-ui:s [AppTheme] (Material 3 Expressive, `MotionScheme.expressive()`, UI-12)
 * med Fonders fasta palett och [FonderTypography] – ingen dynamisk färg, identiteten nordisk blå och koppar
 * ska vara konsekvent (skill `ui-style`).
 *
 * Former, kort, knappar och tillbakapil har bibliotekets standard (samma som ReseApoteket och Dagboken, UI-13,
 * UI-14): 22 dp kort, 56 dp knappar utan formmorf och den tonade pilplattan – Fonder sätter ingen egen stil.
 * Kortens kant och skugga kommer från djupet ([FonderLightDepth]/[FonderDarkDepth], UI-18). Appens identitet
 * (`LocalAppIdentity`: ikonen och namnet) ger flikarna Kodhäst-sidhuvudet (NAV-7).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FonderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    AppTheme(
        colorScheme = if (darkTheme) FonderDarkColorScheme else FonderLightColorScheme,
        extendedColors = if (darkTheme) FonderDarkExtendedColors else FonderLightExtendedColors,
        typeScale = FonderTypography,
        motionScheme = MotionScheme.expressive(),
        depth = if (darkTheme) FonderDarkDepth else FonderLightDepth
    ) {
        CompositionLocalProvider(
            LocalFonderColors provides if (darkTheme) FonderDarkColors else FonderLightColors,
            LocalAppBrandStyle provides FonderBrandStyle,
            LocalAppIdentity provides AppIdentity(R.drawable.ic_app_header, stringResource(R.string.app_name)),
            content = content
        )
    }
}

/**
 * Kodhäst-igenkänningen (UI-17): hästen i tomma lägens ikonruta, prickarna före sektionsrubriker
 * utan ikonruta och prickarna i sidhuvudet som hoppar medan appen hämtar i bakgrunden (NAV-6); flikraden
 * i appens ljusa ton med plusknappens val som FAB-meny i kopparen (NAV-8). Prickarna som laddning står av –
 * Fonder behåller sin formskiftande laddning (UI-12).
 */
private val FonderBrandStyle = AppBrandStyle(
    sectionDots = true,
    emptyStateMark = true,
    headerDots = true,
    toolbarStyle = ToolbarStyle.Tonal,
    actionMenu = ActionMenuStyle.Fab
)

/**
 * Kortstilen för ett kort vars rader själva går ut i kanten och har sin luft innanför klickytan (Facits
 * utfällbara rader, Besluts bytesrader och Facit-raden): ingen inre luft. Allt annat tar bibliotekets standard.
 */
val flushCardStyle = AppCardStyle(padding = 0.dp, segmentGap = 0.dp)

/** Semantiska avkastningsfärger. Får aldrig bäras av enbart färg — para med tecken/pil. */
object ReturnColors {
    val gain: Color
        @Composable @ReadOnlyComposable
        get() = LocalFonderColors.current.gain

    val loss: Color
        @Composable @ReadOnlyComposable
        get() = LocalFonderColors.current.loss

    /** Tonad behållare i vinstens kulör (märken som "Köpt", BYT-4) — para med [onGainContainer]. */
    val gainContainer: Color
        @Composable @ReadOnlyComposable
        get() = AppTheme.tone(Tone.Gain).container

    /** Text på [gainContainer]. */
    val onGainContainer: Color
        @Composable @ReadOnlyComposable
        get() = AppTheme.tone(Tone.Gain).content

    /** Färg för ett värde: vinst om >= 0, annars förlust. */
    @Composable @ReadOnlyComposable
    fun forAmount(amount: Double): Color = if (amount >= 0.0) gain else loss
}

/**
 * Grön/gul/röd statusfärger för säljsignaler (issue #16, ANA-3) — samma fasta palett som
 * övriga appen (UI-1): grön/röd återanvänder [ReturnColors], gul återanvänder den befintliga
 * kopparaccenten (`secondary`) i stället för en ny hårdkodad färg.
 */
object StatusColors {
    val gron: Color
        @Composable @ReadOnlyComposable
        get() = ReturnColors.gain

    val gul: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.secondary

    val rod: Color
        @Composable @ReadOnlyComposable
        get() = ReturnColors.loss
}

/**
 * Kurvfärger i jämförelsediagrammet (ANA-11, issue #85) — innehavet mot en föreslagen fond.
 *
 * Två grannar i samma primärskala går i praktiken inte att skilja åt, så kurvorna skiljs på
 * **ljushet**, inte bara i kulör: innehavet är primärblått och heldraget, jämförelsen koppar och streckad. Köpmarkörerna är också koppar men lodräta, så de förväxlas inte med kurvan.
 * Skillnaden håller därmed i båda temana.
 *
 * Kopparaccenten delas med köpmarkörerna (issue #55), men de är lodräta linjer med datum och
 * jämförelsekurvan är streckad, så de går inte att förväxla.
 */
object ChartSeriesColors {
    /** Innehavets egen kurva — primärfärgen (mörkblå i ljust tema, ljusblå i mörkt, UI-1). */
    val holding: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    /** Jämförelsekurvan (referens/kandidat) — koppar och streckad i diagrammet, så den skiljer sig från innehavet i både kulör och form. */
    val candidate: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.secondary

    /** 200-dagarssnittet (ANA-14) — dämpat `onSurfaceVariant` och prickat, så det varken förväxlas med kursen eller jämförelsen. */
    val movingAverage: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant
}
