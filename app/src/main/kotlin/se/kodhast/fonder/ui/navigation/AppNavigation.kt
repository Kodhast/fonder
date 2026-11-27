package se.kodhast.fonder.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import se.kodhast.fonder.BuildConfig
import se.kodhast.fonder.R
import se.kodhast.fonder.data.auth.AuthUser
import se.kodhast.fonder.ui.beslut.BeslutScreen
import se.kodhast.fonder.ui.bytesfonster.SwitchWatchScreen
import se.kodhast.fonder.ui.components.ComponentGallery
import se.kodhast.fonder.ui.facit.FacitScreen
import se.kodhast.fonder.ui.fond.FondDetaljScreen
import se.kodhast.fonder.ui.fondsok.FundSearchScreen
import se.kodhast.fonder.ui.hem.HemScreen
import se.kodhast.fonder.ui.imports.ImportHoldingsScreen
import se.kodhast.fonder.ui.imports.ImportOrdersScreen
import se.kodhast.fonder.ui.portfolj.PortfoljScreen
import se.kodhast.fonder.ui.riskprofil.RiskProfilScreen
import se.kodhast.fonder.ui.settings.AboutScreen
import se.kodhast.fonder.ui.settings.AccountViewModel
import se.kodhast.fonder.ui.settings.SettingsScreen
import se.kodhast.fonder.ui.transaktioner.TransactionFormScreen
import se.kodhast.fonder.ui.transaktioner.TransaktionerScreen
import se.kodhast.ui.common.SyncUiState
import se.kodhast.ui.common.UiError
import se.kodhast.ui.common.UpdateChecker
import se.kodhast.ui.components.AccountInfo
import se.kodhast.ui.components.AccountMenuAvatar
import se.kodhast.ui.components.AppMenuItem
import se.kodhast.ui.components.AppSnackbarHost
import se.kodhast.ui.components.AppUpdateNotice
import se.kodhast.ui.components.ErrorSnackbar
import se.kodhast.ui.components.FloatingToolbarClearance
import se.kodhast.ui.components.LocalAppUpdate
import se.kodhast.ui.components.R as UiR
import se.kodhast.ui.components.ToolbarAction
import se.kodhast.ui.components.ToolbarItem
import se.kodhast.ui.components.rememberAppUpdate
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
    decisionBadgeViewModel: DecisionBadgeViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel()
) {
    val isRunning by workerStatusViewModel.isRunning.collectAsStateWithLifecycle()
    val beslutBadgeCount by decisionBadgeViewModel.count.collectAsStateWithLifecycle()
    val user by accountViewModel.user.collectAsStateWithLifecycle()
    val online by accountViewModel.online.collectAsStateWithLifecycle()
    val accountError by accountViewModel.error.collectAsStateWithLifecycle()

    // Engångsbegäran från Översiktens påminnelse: Inställningar skrollar fram säkerhetskopieringen
    // och kvitterar. Sparad så den överlever rotation mellan trycket och skrollningen.
    val scrollSettingsToBackup = rememberSaveable { mutableStateOf(false) }
    // Ett val i Fondsöks valläge, på väg till skärmen under (FundPick).
    val fundPick = rememberFundPick()
    val backStack = rememberAppBackStack()
    val tabOwner = checkNotNull(LocalViewModelStoreOwner.current) { "AppNavigation kräver en ViewModelStoreOwner" }
    val entries = remember(backStack, tabOwner) { fonderEntries(backStack, scrollSettingsToBackup, fundPick, tabOwner) }

    val context = LocalContext.current
    FonderNavHost(
        backStack = backStack,
        entryProvider = entries,
        modifier = modifier,
        isWorkerRunning = isRunning,
        beslutBadgeCount = beslutBadgeCount,
        accountError = accountError,
        onAccountErrorShow = accountViewModel::onErrorShown,
        // Samma lambda tills kontot eller anslutningen ändras, så att flikarnas sidhuvud inte ritas om i onödan.
        account = remember(user, online, backStack) {
            {
                FonderAccountMenu(
                    user = user,
                    backStack = backStack,
                    online = online,
                    onSignIn = { accountViewModel.signIn(context) },
                    onSignOut = accountViewModel::signOut
                )
            }
        }
    )
}

/**
 * Avatarmenyn i flikarnas sidhuvud (NAV-7): bibliotekets [AccountMenuAvatar] med Fonders mål – Inställningar, Om Fonder
 * och i debug-bygget komponentgalleriet ([showGallery]). [online] är pluppen på avataren (bara inloggad). Egen funktion
 * så att testerna prövar samma koppling som appen.
 */
@Composable
fun FonderAccountMenu(
    user: AuthUser?,
    backStack: AppBackStack,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    online: Boolean? = null,
    showGallery: Boolean = BuildConfig.DEBUG
) {
    AccountMenuAvatar(
        user?.let { AccountInfo(it.displayName, it.email, photoUrl = null) },
        stringResource(R.string.app_name),
        onOpenSettings = { backStack.push(SettingsKey) },
        onOpenAbout = { backStack.push(AboutKey) },
        onSignIn = onSignIn,
        onSignOut = onSignOut,
        signInNote = stringResource(R.string.account_sign_in_note),
        online = online,
        onOpenGallery = if (showGallery) ({ backStack.push(ComponentGalleryKey) }) else null
    )
}

/**
 * Navigationsramen med Fonders flikrad (NAV-1, NAV-7): fyra flikar, kopparmärket med antalet beslut på
 * Beslut ([beslutBadgeCount], 0 ritar inget, NAV-9) och plusknappens FAB-meny (NAV-8). [isWorkerRunning]
 * får sidhuvudets prickar att hoppa (NAV-6). [account] står längst till höger i flikarnas sidhuvud (avatarmenyn,
 * NAV-7); [accountError] visas en gång som snackbar. [entryProvider] ger skärmarna – i appen [fonderEntries], i
 * testerna enkla ersättare. Varje skärm står på en av android-ui:s ramar och ritar sin egen topprad (#266 F6).
 */
@Composable
fun FonderNavHost(
    backStack: AppBackStack,
    entryProvider: (AppKey) -> NavEntry<AppKey>,
    modifier: Modifier = Modifier,
    isWorkerRunning: Boolean = false,
    beslutBadgeCount: Int = 0,
    accountError: UiError? = null,
    onAccountErrorShow: () -> Unit = {},
    account: @Composable () -> Unit = {}
) {
    val tabs = Screen.entries.map { tab ->
        ToolbarItem(
            label = stringResource(tab.labelRes),
            icon = tab.icon,
            badgeCount = beslutBadgeCount.takeIf { tab == Screen.Beslut }
        )
    }
    // Ett inloggningsfel från avatarmenyn visas en gång som snackbar över flikraden (TP-6). Inte ramens sync.writeError:
    // den skriver "En ändring kunde inte sparas och har ångrats" före orsaken.
    val messages = remember { SnackbarHostState() }
    ErrorSnackbar(accountError, messages, onShown = onAccountErrorShow)
    Box(modifier.fillMaxSize()) {
        // Ny version (NAV-10): releasebygget frågar GitHub; pricken i sidhuvudet och Om-raden öppnar Om, där Hämta finns.
        val checker = remember { if (BuildConfig.DEBUG) null else UpdateChecker("Kodhast", "fonder", BuildConfig.VERSION_NAME) }
        val update by rememberAppUpdate(checker)
        val notice = remember(update, backStack) { update?.let { AppUpdateNotice(it) { backStack.push(AboutKey) } } }
        CompositionLocalProvider(LocalAppUpdate provides notice) {
            AppNavHost(
                backStack = backStack,
                tabs = tabs,
                // Samma innehållsnyckel som skärmens egen post, så att ramens sparade flikläge (rullning) gäller.
                entryProvider = { key ->
                    val entry = entryProvider(key)
                    NavEntry(key, contentKey = entry.contentKey, metadata = entry.metadata) { entry.Content() }
                },
                modifier = Modifier.fillMaxSize(),
                sync = SyncUiState(pending = isWorkerRunning),
                action = ToolbarAction(stringResource(R.string.fab_menu_open), UiR.drawable.ic_add, addMenu(backStack)),
                account = account
            )
        }
        AppSnackbarHost(
            messages,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = FloatingToolbarClearance)
        )
    }
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
 * (AppKey är förseglad; en saknad är ett programfel). En skärm som stänger sig själv gör det med `popIfTop(key)`, så att ett
 * andra anrop inte stänger skärmen under. Flikarnas ViewModels hämtas från [tabOwner] (aktiviteten), så att
 * en flik behåller sitt innehåll när man byter flik; undersidornas hör till sin post. [scrollSettingsToBackup]
 * läses när Inställningar ritas, inte när posten skapas, så kvitteringen slår igenom direkt. [fundPick] bär
 * Fondsöks val i valläget till skärmen under, som sparar det med sin egen ViewModel ([FundPick]).
 */
fun fonderEntries(
    backStack: AppBackStack,
    scrollSettingsToBackup: MutableState<Boolean>,
    fundPick: MutableState<FundPick?>,
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
    entry<FacitKey> { key ->
        FacitScreen(onBack = { backStack.popIfTop(key) })
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
            scrollToBackup = scrollSettingsToBackup.value,
            onScrolledToBackup = { scrollSettingsToBackup.value = false },
            pickedBenchmark = (fundPick.value as? FundPick.Benchmark)?.fund,
            onPickedBenchmarkAcknowledge = { fundPick.value = null }
        )
    }
    entry<AboutKey> { key ->
        AboutScreen(onBack = { backStack.popIfTop(key) })
    }
    // Komponentgalleriet – bara nåbart i debug-bygget (avatarmenyn visar raden bara då).
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
        ImportHoldingsScreen(onBack = { backStack.popIfTop(key) })
    }
    entry<ImportOrdersKey> { key ->
        ImportOrdersScreen(onBack = { backStack.popIfTop(key) })
    }
    entry<RiskProfileKey> { key ->
        RiskProfilScreen(onClose = { backStack.popIfTop(key) })
    }
    // Fondsök i valläge (HEM-10, issue #102): samma skärm, men träffraden pekar ut en
    // jämförelsefond i stället för att lägga till den i bevakningen. Valet går tillbaka till
    // Inställningar ([FundPick]), vars ViewModel redan äger inställningarna — fondsök förblir en sökskärm.
    entry<BenchmarkPickerKey> { key ->
        FundSearchScreen(
            title = stringResource(R.string.settings_benchmark_section),
            onBack = { backStack.popIfTop(key) },
            onPickFund = { fund ->
                fundPick.value = FundPick.Benchmark(fund)
                backStack.popIfTop(key)
            }
        )
    }
    entry<SwitchWatchKey> { key ->
        SwitchWatchScreen(
            watchId = key.watchId,
            onAddCandidate = { backStack.push(SwitchWatchPickerKey(key.watchId)) },
            onBuy = { isin -> backStack.push(TransactionFormKey.bought(key.watchId, isin)) },
            onClose = { backStack.popIfTop(key) },
            pickedCandidate = (fundPick.value as? FundPick.Candidate)?.takeIf { it.watchId == key.watchId }?.fund,
            onPickedCandidateAcknowledge = { fundPick.value = null }
        )
    }
    // Fondsök i valläge (ANA-13): samma skärm som HEM-10:s referensval, men träffen
    // läggs till som bevakat alternativ i stället för att bli ny bevakad fond. Valet går
    // tillbaka till Pågående byte för bevakningen i nyckeln ([FundPick]).
    entry<SwitchWatchPickerKey> { key ->
        FundSearchScreen(
            title = stringResource(R.string.switch_watch_add_candidate),
            onBack = { backStack.popIfTop(key) },
            onPickFund = { fund ->
                fundPick.value = FundPick.Candidate(key.watchId, fund)
                backStack.popIfTop(key)
            }
        )
    }
}
