package se.kodhast.fonder.data.common

import se.kodhast.fonder.ui.common.toMessage
import se.kodhast.ui.common.ErrorMapper
import se.kodhast.ui.common.UiError

/**
 * Den enda översättningen från datalagrets fel till android-ui:s [UiError] (NFR-6): orsaken är
 * [DataError.of], texten `DataError.toMessage()`, och något som inte finns längre
 * ([DataError.NotFound]) erbjuder inte "Försök igen".
 */
object AppErrors : ErrorMapper {
    override fun map(error: Throwable): UiError {
        val known = DataError.of(error)
        return UiError(known, known.toMessage(), retryable = known != DataError.NotFound)
    }

    override fun notFound(): UiError = map(DataError.NotFound)
}
