package se.kodhast.fonder.domain.model

/** Fondnamn + valuta för ett exakt känt ISIN, från en [se.kodhast.fonder.data.network.IsinPriceHistorySource]. */
data class IsinFundInfo(
    val name: String,
    val currency: String
)
