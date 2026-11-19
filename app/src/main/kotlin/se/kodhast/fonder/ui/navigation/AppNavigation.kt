package se.kodhast.fonder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import se.kodhast.fonder.BuildConfig
import se.kodhast.fonder.R
import se.kodhast.fonder.ui.beslut.BeslutScreen
import se.kodhast.fonder.ui.bytesfonster.SwitchWatchEvent
import se.kodhast.fonder.ui.bytesfonster.SwitchWatchScreen
import se.kodhast.fonder.ui.bytesfonster.SwitchWatchViewModel
import se.kodhast.fonder.ui.components.ComponentGallery
import se.kodhast.fonder.ui.facit.FacitScreen
import se.kodhast.fonder.ui.fond.FondDetaljScreen
import se.kodhast.fonder.ui.fondsok.FundSearchScreen
import se.kodhast.fonder.ui.hem.HemScreen
import se.kodhast.fonder.ui.imports.ImportHoldingsScreen
import se.kodhast.fonder.ui.imports.ImportOrdersScreen
import se.kodhast.fonder.ui.portfolj.PortfoljScreen
import se.kodhast.fonder.ui.riskprofil.RiskProfilScreen
import se.kodhast.fonder.ui.settings.SettingsEvent
import se.kodhast.fonder.ui.settings.SettingsScreen
import se.kodhast.fonder.ui.settings.SettingsViewModel
import se.kodhast.fonder.ui.transaktioner.TransactionFormScreen
import se.kodhast.fonder.ui.transaktioner.TransaktionerScreen
import se.kodhast.ui.common.SyncUiState
import se.kodhast.ui.components.AppMenuItem
import se.kodhast.ui.components.R as UiR
import se.kodhast.ui.components.ToolbarAction
import se.kodhast.ui.components.ToolbarItem
import se.kodhast.ui.navigation.AppNavHost

/**
 * Appens navigering: android-ui:s navigationsram ([AppNavHost]) med Fonders flikar, plusmenyn och
 * skärmarna ([fonderEntries]). Bakgrundsjobbet (kursuppdateringen, NAV-6) får prickarna i sidhuvudet att
 * hoppa; antalet väntande beslut står på Beslut (NAV-9).
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    workerStatusViewModel: WorkerStatusViewModel = hiltViewModel(),
    decisionBadgeViewModel: DecisionBadgeViewModel = hiltViewModel()
) {
    val isRunning by workerStatusViewModel.isRunning.collectAsStateWithLifecycle()
    val beslutBadgeCount by decisionBadgeViewModel.count.collectAsStateWithLifecycle()

    // Engångsbegäran från Översiktens påminnelse: Inställningar skrollar fram säkerhetskopieringen
    // och kvitterar. Sparad så den överlever rotation mellan trycket och skrollningen.
    val scrollSettingsToBackup = rememberSaveable { mutableStateOf(false) }
    val backStack = rememberAppBackStack()
    val tabOwner = checkNotNull(LocalViewModelStoreOwner.current) { "AppNavigation kräver en ViewModelStoreOwner" }
    val entries = remember(backStack, tabOwner) { fonderEntries(backStack, scrollSettingsToBackup, tabOwner) }

    FonderNavHost(
        backStack = backStack,
        entryProvider = entries,
        modifier = modifier,
        isWorkerRunning = isRunning,
        beslutBadgeCount = beslutBadgeCount
    )
}

/**
 * Navigationsramen med Fonders flikrad (NAV-1, NAV-7): fyra flikar, kopparmärket med antalet beslut på
 * Beslut ([beslutBadgeCount], 0 ritar inget, NAV-9) och plusknappens FAB-meny (NAV-8). [isWorkerRunning]
 * får sidhuvudets prickar att hoppa (NAV-6). [entryProvider] ger skärmarna – i appen [fonderEntries], i
 * testerna enkla ersättare – och ramen lägger toppraden runt dem som inte har en egen ([FonderPage]).
 */
@Composable
fun FonderNavHost(
    backStack: AppBackStack,
    entryProvider: (AppKey) -> NavEntry<AppKey>,
    modifier: Modifier = Modifier,
    isWorkerRunning: Boolean = false,
    beslutBadgeCount: Int = 0
) {
    val tabs = Screen.entries.map { tab ->
        ToolbarItem(
            label = stringResource(tab.labelRes),
            icon = tab.icon,
            badgeCount = beslutBadgeCount.takeIf { tab == Screen.Beslut }
        )
    }
    AppNavHost(
        backStack = backStack,
        tabs = tabs,
        // Samma innehållsnyckel som skärmens egen post, så att ramens sparade flikläge (rullning) gäller.
        entryProvider = { key ->
            val entry = entryProvider(key)
            NavEntry(key, contentKey = entry.contentKey, metadata = entry.metadata) { FonderPage(key, backStack) { entry.Content() } }
        },
        modifier = modifier,
        sync = SyncUiState(pending = isWorkerRunning),
        action = ToolbarAction(stringResource(R.string.fab_menu_open), UiR.drawable.ic_add, addMenu(backStack))
    )
}

/**
 * Toppraden runt [key]s skärm (UI-8): på en underskärm utan ram den lilla toppraden med rubrik och
 * tillbakapil ([SubPage], [subPageTitle]). Flikarna och skärmarna på android-ui:s ramar (Översikt, Innehav,
 * Beslut, Händelser, Fonddetalj, Fondsök, formulären, galleriet) ritar sin egen och får ingen.
 */
@Composable
fun FonderPage(key: AppKey, backStack: AppBackStack, content: @Composable () -> Unit) {
    val subTitle = subPageTitle(key)
    if (subTitle != null) SubPage(stringResource(subTitle), onBack = { backStack.popIfTop(key) }, content = content) else content()
}

/** Rubriken för en underskärm utan ram ([SubPage]); `null` för flikarna och skärmarna på en ram. */
fun subPageTitle(key: AppKey): Int? = when (key) {
    ImportHoldingsKey -> R.string.import_title

    ImportOrdersKey -> R.string.import_orders_title

    FacitKey -> R.string.facit_title

    is SwitchWatchKey -> R.string.switch_watch_title

    is TabKey, SettingsKey, is FondDetaljKey, FundSearchKey, is TransactionFormKey, RiskProfileKey, BenchmarkPickerKey,
    is SwitchWatchPickerKey, ComponentGalleryKey -> null
}

/** Plusknappens val (NAV-8) i samma ordning som förut, med ikoner som i FAB-menyn. */
@Composable
private fun addMenu(backStack: AppBackStack): List<AppMenuItem> = listOf(
    AppMenuItem(stringResource(R.string.transaktioner_add), { backStack.push(TransactionFormKey()) }, icon = R.drawable.ic_receipt),
    AppMenuItem(stringResource(R.string.fab_import_orders), { backStack.push(ImportOrdersKey) }, icon = R.drawable.ic_upload),
    AppMenuItem(stringResource(R.string.fab_import_holdings), { backStack.push(ImportHoldingsKey) }, icon = R.drawable.ic_holdings),
    AppMenuItem(stringResource(R.string.fab_watch_fund), { backStack.push(FundSearchKey) }, icon = R.drawable.ic_search)
)

/**
 * Fonders skärmar: de fyra flikarna och alla skärmar djupare i stacken, en per nyckel – varje nyckel har en
 * (AppKey är förseglad; en saknad är ett programfel). Toppraden runt skärmarna utan ram lägger
 * [FonderNavHost] till ([FonderPage]). En skärm som stänger sig själv gör det med `popIfTop(key)`, så att ett
 * andra anrop inte stänger skärmen under. Flikarnas ViewModels hämtas från [tabOwner] (aktiviteten), så att
 * en flik behåller sitt innehåll när man byter flik; undersidornas hör till sin post. [scrollSettingsToBackup]
 * läses när Inställningar ritas, inte när posten skapas, så kvitteringen slår igenom direkt.
 */
fun fonderEntries(
    backStack: AppBackStack,
    scrollSettingsToBackup: MutableState<Boolean>,
    tabOwner: ViewModelStoreOwner
): (AppKey) -> NavEntry<AppKey> = entryProvider(fallback = { key -> error("Ingen skärm för $key") }) {
    // Påminnelsen (HEM-12) och det tomma lägets "Återställ från säkerhetskopia" (HEM-15) går samma väg.
    val openBackup = {
        scrollSettingsToBackup.value = true
        backStack.push(SettingsKey)
    }
    entry<OversiktKey> {
        HemScreen(
            onFundClick = { fundId -> backStack.push(FondDetaljKey(fundId)) },
            onOpenBeslut = { backStack.select(BeslutKey) },
            onOpenSettings = { backStack.push(SettingsKey) },
            onOpenBackup = openBackup,
            onWatchFund = { backStack.push(FundSearchKey) },
            onImportHoldings = { backStack.push(ImportHoldingsKey) },
            viewModel = hiltViewModel(tabOwner)
        )
    }
    entry<InnehavKey> {
        PortfoljScreen(
            onFundClick = { fundId -> backStack.push(FondDetaljKey(fundId)) },
            onNewTransaction = { backStack.push(TransactionFormKey()) },
            onImportOrders = { backStack.push(ImportOrdersKey) },
            viewModel = hiltViewModel(tabOwner)
        )
    }
    entry<BeslutKey> {
        BeslutScreen(
            onOpenSwitchWatch = { watchId -> backStack.push(SwitchWatchKey(watchId)) },
            onOpenRiskProfile = { backStack.push(RiskProfileKey) },
            onOpenFacit = { backStack.push(FacitKey) },
            onFollow = { suggestion -> backStack.push(TransactionFormKey.follow(suggestion)) },
            viewModel = hiltViewModel(tabOwner)
        )
    }
    // Facit (SET-5) — en skärm i stacken med bakåtpil, från Beslut och från Inställningar (NAV-9).
    entry<FacitKey> {
        FacitScreen()
    }
    entry<HandelserKey> {
        TransaktionerScreen(
            onOpenSwitchWatch = { watchId -> backStack.push(SwitchWatchKey(watchId)) },
            onNewTransaction = { backStack.push(TransactionFormKey()) },
            onImportOrders = { backStack.push(ImportOrdersKey) },
            viewModel = hiltViewModel(tabOwner)
        )
    }
    entry<SettingsKey> { key ->
        SettingsScreen(
            onBack = { backStack.popIfTop(key) },
            onImportHoldings = { backStack.push(ImportHoldingsKey) },
            onImportOrders = { backStack.push(ImportOrdersKey) },
            onOpenRiskProfile = { backStack.push(RiskProfileKey) },
            onOpenFacit = { backStack.push(FacitKey) },
            onOpenBenchmarkPicker = { backStack.push(BenchmarkPickerKey) },
            onOpenGallery = if (BuildConfig.DEBUG) ({ backStack.push(ComponentGalleryKey) }) else null,
            scrollToBackup = scrollSettingsToBackup.value,
            onScrolledToBackup = { scrollSettingsToBackup.value = false }
        )
    }
    // Komponentgalleriet – bara nåbart i debug-bygget (Inställningar visar länken bara då).
    entry<ComponentGalleryKey> { key ->
        ComponentGallery(onBack = { backStack.popIfTop(key) })
    }
    entry<FondDetaljKey> { key ->
        FondDetaljScreen(
            fundId = key.fundId,
            onBack = { backStack.popIfTop(key) },
            onOpenSwitchWatch = { watchId -> backStack.push(SwitchWatchKey(watchId)) },
            onOpenTransactionForm = { formKey -> backStack.push(formKey) }
        )
    }
    entry<FundSearchKey> { key ->
        FundSearchScreen(title = stringResource(R.string.fondsok_title), onBack = { backStack.popIfTop(key) })
    }
    entry<TransactionFormKey> { key ->
        TransactionFormScreen(formKey = key, onClose = { backStack.popIfTop(key) }, onWatchFund = { backStack.push(FundSearchKey) })
    }
    entry<ImportHoldingsKey> { key ->
        ImportHoldingsScreen(onDone = { backStack.popIfTop(key) })
    }
    entry<ImportOrdersKey> { key ->
        ImportOrdersScreen(onDone = { backStack.popIfTop(key) })
    }
    entry<RiskProfileKey> { key ->
        RiskProfilScreen(onClose = { backStack.popIfTop(key) })
    }
    // Fondsök i valläge (HEM-10, issue #102): samma skärm, men träffraden pekar ut en
    // jämförelsefond i stället för att lägga till den i bevakningen. Valet skrivs av
    // SettingsViewModel, som redan äger inställningarna — fondsök förblir en sökskärm.
    entry<BenchmarkPickerKey> { key ->
        val settingsViewModel: SettingsViewModel = hiltViewModel()
        FundSearchScreen(
            title = stringResource(R.string.settings_benchmark_section),
            onBack = { backStack.popIfTop(key) },
            onPickFund = { fund ->
                settingsViewModel.onEvent(SettingsEvent.ChooseBenchmark(fund))
                backStack.popIfTop(key)
            }
        )
    }
    entry<SwitchWatchKey> { key ->
        SwitchWatchScreen(
            watchId = key.watchId,
            onAddCandidate = { backStack.push(SwitchWatchPickerKey(key.watchId)) },
            onBuy = { isin -> backStack.push(TransactionFormKey.bought(key.watchId, isin)) },
            onClose = { backStack.popIfTop(key) }
        )
    }
    // Fondsök i valläge (ANA-13): samma skärm som HEM-10:s referensval, men träffen
    // läggs till som bevakat alternativ i stället för att bli ny bevakad fond. Egen
    // ViewModel för posten — den får bevakningens id ur sin nyckel.
    entry<SwitchWatchPickerKey> { key ->
        val switchWatchViewModel = hiltViewModel<SwitchWatchViewModel, SwitchWatchViewModel.Factory> {
            it.create(key.watchId)
        }
        FundSearchScreen(
            title = stringResource(R.string.switch_watch_add_candidate),
            onBack = { backStack.popIfTop(key) },
            onPickFund = { fund ->
                switchWatchViewModel.onEvent(SwitchWatchEvent.AddCandidate(fund))
                backStack.popIfTop(key)
            }
        )
    }
}
