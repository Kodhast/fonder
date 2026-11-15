package se.kodhast.fonder.testing

import se.kodhast.fonder.data.repository.FakeCachedPriceRepository
import se.kodhast.fonder.data.repository.FakeSuggestionRecordRepository
import se.kodhast.fonder.data.repository.FundPriceRepository
import se.kodhast.fonder.data.repository.SuggestionRecordRepository
import se.kodhast.fonder.data.repository.SwitchWatchRepository
import se.kodhast.fonder.data.repository.TransactionRepository
import se.kodhast.fonder.domain.model.RiskProfile
import se.kodhast.fonder.domain.usecase.TargetLevelPreselection

/**
 * En riktig [TargetLevelPreselection] (issue #150) på fakes, med riskprofilen given direkt i
 * stället för via DataStore. Utan [riskProfile] läses inget annat än profilen, och förvalet blir
 * säljfondens egen nivå — beteendet före #150, vilket de flesta tester av andra saker vill ha.
 */
fun testTargetLevelPreselection(
    transactions: TransactionRepository,
    switchWatches: SwitchWatchRepository,
    riskProfile: RiskProfile? = null,
    prices: FundPriceRepository = FakeCachedPriceRepository(),
    records: SuggestionRecordRepository = FakeSuggestionRecordRepository()
): TargetLevelPreselection = TargetLevelPreselection({ riskProfile }, transactions, prices, switchWatches, records)
