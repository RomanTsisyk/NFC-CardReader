package io.github.romantsisyk.nfccardreader.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NfcResult sealed class.
 */
class NfcResultTest {

    @Test
    fun `Success result should return correct data`() {
        val data = "test data"
        val result: NfcResult<String> = NfcResult.Success(data)

        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertFalse(result.isLoading)
        assertEquals(data, result.getOrNull())
        assertNull(result.errorOrNull())
    }

    @Test
    fun `Error result should return correct error`() {
        val error = NfcError.TAG_NOT_FOUND
        val message = "No tag found"
        val result: NfcResult<String> = NfcResult.Error(error, message)

        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertFalse(result.isLoading)
        assertNull(result.getOrNull())
        assertEquals(error, result.errorOrNull())
    }

    @Test
    fun `Error result with exception should preserve exception`() {
        val error = NfcError.COMMUNICATION_ERROR
        val message = "Communication failed"
        val exception = RuntimeException("Test exception")
        val result = NfcResult.Error(error, message, exception)

        assertTrue(result is NfcResult.Error)
        assertEquals(exception, (result as NfcResult.Error).exception)
    }

    @Test
    fun `Loading result should have correct state`() {
        val result: NfcResult<String> = NfcResult.Loading

        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertTrue(result.isLoading)
        assertNull(result.getOrNull())
        assertNull(result.errorOrNull())
    }

    @Test
    fun `all NfcError values should be accessible`() {
        val errors = NfcError.entries

        assertTrue(errors.contains(NfcError.NFC_NOT_AVAILABLE))
        assertTrue(errors.contains(NfcError.NFC_DISABLED))
        assertTrue(errors.contains(NfcError.TAG_NOT_FOUND))
        assertTrue(errors.contains(NfcError.UNSUPPORTED_TAG))
        assertTrue(errors.contains(NfcError.COMMUNICATION_ERROR))
        assertTrue(errors.contains(NfcError.PARSE_ERROR))
        assertTrue(errors.contains(NfcError.INVALID_DATA))
        assertTrue(errors.contains(NfcError.INITIALIZATION_ERROR))
        assertTrue(errors.contains(NfcError.UNKNOWN_ERROR))
    }
}
