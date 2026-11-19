package se.kodhast.fonder.ui.common

import app.cash.turbine.test
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.kodhast.fonder.data.common.AppErrors
import se.kodhast.fonder.data.common.DataError
import se.kodhast.ui.common.catchAsDetailError
import se.kodhast.ui.common.catchAsListError

/** Fonders felmappning ([AppErrors]) kopplad till android-ui:s tillståndshjälpare (NFR-6). */
class ListStatesTest {

    @Test
    fun `ett fel i källan blir skärmens tillstånd med appens mappade fel i stället för en krasch`() = runTest {
        val source = flow<String> {
            emit("laddad")
            throw DataError.Database
        }

        source.catchAsListError(AppErrors) { "fel: ${it.error.cause}" }.test {
            assertEquals("laddad", awaitItem())
            assertEquals("fel: ${DataError.Database}", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `ett fel i en detaljskärms källa mappas på samma sätt`() = runTest {
        val source = flow<String> { throw DataError.Network }

        source.catchAsDetailError(AppErrors) { "fel: ${it.error.cause}" }.test {
            assertEquals("fel: ${DataError.Network}", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `felet mappas till appens text och försök igen finns utom för saknad post`() {
        assertEquals(DataError.Database.toMessage(), AppErrors.map(DataError.Database).message)
        assertTrue(AppErrors.map(DataError.Network).retryable)
        assertTrue(!AppErrors.notFound().retryable)
    }
}
