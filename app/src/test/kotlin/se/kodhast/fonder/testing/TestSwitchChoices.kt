package se.kodhast.fonder.testing

import java.time.LocalDate
import se.kodhast.fonder.data.datastore.PreferencesRepository
import se.kodhast.fonder.data.repository.FakeCachedPriceRepository
import se.kodhast.fonder.data.repository.FakeFundMetadataRepository
import se.kodhast.fonder.data.repository.FakeSuggestionDismissalRepository
import se.kodhast.fonder.data.repository.FakeSuggestionRecordRepository
import se.kodhast.fonder.data.repository.FakeSwitchWatchRepository
import se.kodhast.fonder.data.repository.FundPriceRepository
import se.kodhast.fonder.domain.usecase.DismissedPlanSource
import se.kodhast.fonder.domain.usecase.InProgressSwitches
import se.kodhast.fonder.domain.usecase.PendingDecisionsSource
import se.kodhast.fonder.domain.usecase.SwitchChoicesSource

/**
 * En riktig [PendingDecisionsSource] på fakes — samma uppställning som Beslut läser, delad så att
 * varje test inte bygger kedjan själv (skill testing-strategy → DRY i tester).
 */
fun testPendingDecisionsSource(
    preferences: PreferencesRepository,
    suggestionRepo: FakeSuggestionRecordRepository = FakeSuggestionRecordRepository(),
    switchWatchRepo: FakeSwitchWatchRepository = FakeSwitchWatchRepository(),
    priceRepo: FundPriceRepository = FakeCachedPriceRepository(),
    metadataRepo: FakeFundMetadataRepository = FakeFundMetadataRepository(),
    dismissalRepo: FakeSuggestionDismissalRepository = FakeSuggestionDismissalRepository()
): PendingDecisionsSource = PendingDecisionsSource(
    preferences,
    suggestionRepo,
    switchWatchRepo,
    priceRepo,
    metadataRepo,
    DismissedPlanSource(suggestionRepo, dismissalRepo, preferences, metadataRepo),
    InProgressSwitches(switchWatchRepo, suggestionRepo, preferences)
)

/** Bytesväljarens indata (BYT-2) ur samma fakes — formulärets och PDF-importens tester. */
fun testSwitchChoicesSource(
    preferences: PreferencesRepository,
    suggestionRepo: FakeSuggestionRecordRepository = FakeSuggestionRecordRepository(),
    switchWatchRepo: FakeSwitchWatchRepository = FakeSwitchWatchRepository(),
    priceRepo: FundPriceRepository = FakeCachedPriceRepository(),
    metadataRepo: FakeFundMetadataRepository = FakeFundMetadataRepository(),
    dismissalRepo: FakeSuggestionDismissalRepository = FakeSuggestionDismissalRepository(),
    today: () -> LocalDate = LocalDate::now
): SwitchChoicesSource = SwitchChoicesSource(
    testPendingDecisionsSource(preferences, suggestionRepo, switchWatchRepo, priceRepo, metadataRepo, dismissalRepo),
    switchWatchRepo,
    suggestionRepo,
    today
)
