package io.github.romantsisyk.nfccardreader.domain.usecase

import io.github.romantsisyk.nfccardreader.util.createByteArrayFromHex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParseTLVUseCaseTest {

    private lateinit var parseTLVUseCase: ParseTLVUseCase

    @Before
    fun setup() {
        parseTLVUseCase = ParseTLVUseCase()
    }

    @Test
    fun `empty input returns empty map`() {
        assertTrue(parseTLVUseCase.execute(ByteArray(0)).isEmpty())
    }

    @Test
    fun `PAN tag is masked in result`() {
        // Tag 5A = APPLICATION_PAN, length 08, 8 bytes of fake PAN
        val data = createByteArrayFromHex("5A 08 41 11 11 11 11 11 11 11")
        val result = parseTLVUseCase.execute(data)

        val pan = result["APPLICATION_PAN"]
        assertTrue("PAN should be present in result", pan != null)
        assertTrue("PAN should be masked (contain X)", pan!!.contains("X", ignoreCase = true))
        assertTrue("Masked PAN should not be empty", pan.isNotEmpty())
    }

    @Test
    fun `expiration date tag is parsed`() {
        // Tag 5F24 = EXPIRATION_DATE, length 03, bytes 25 02 28
        val data = createByteArrayFromHex("5F 24 03 25 02 28")
        val result = parseTLVUseCase.execute(data)

        val expiry = result["EXPIRATION_DATE"]
        assertTrue("Expiration date should be present", expiry != null)
        assertEquals("250228", expiry)
    }

    @Test
    fun `multiple tags are all parsed`() {
        // PAN (5A) + expiry (5F24)
        val data = createByteArrayFromHex(
            "5A 08 41 11 11 11 11 11 11 11 " +
            "5F 24 03 25 02 28"
        )
        val result = parseTLVUseCase.execute(data)

        assertEquals(2, result.size)
        assertTrue(result.containsKey("APPLICATION_PAN"))
        assertTrue(result.containsKey("EXPIRATION_DATE"))
    }

    @Test
    fun `truncated data does not crash`() {
        // Tag present but length exceeds remaining bytes
        val data = createByteArrayFromHex("5A 10 41 11")
        val result = parseTLVUseCase.execute(data)
        // Should return empty map rather than throw
        assertTrue(result.isEmpty())
    }

    @Test
    fun `unknown tag is stored with Tag prefix`() {
        // Tag FF is UNKNOWN, length 02, 2 bytes
        val data = createByteArrayFromHex("FF 02 AA BB")
        val result = parseTLVUseCase.execute(data)

        assertTrue("Unknown tag should be stored with 'Tag' prefix",
            result.keys.any { it.startsWith("Tag") })
    }
}
