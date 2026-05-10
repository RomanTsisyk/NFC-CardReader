package io.github.romantsisyk.nfccardreader.domain.usecase

import io.github.romantsisyk.nfccardreader.domain.EmvTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * InterpretNfcDataUseCase consumes the parsed TLV map (Map<String, String>) produced by
 * ParseTLVUseCase and translates it into the human-readable NFCData domain object.
 */
class InterpretNfcDataUseCaseTest {

    private lateinit var interpretNfcDataUseCase: InterpretNfcDataUseCase

    @Before
    fun setup() {
        interpretNfcDataUseCase = InterpretNfcDataUseCase()
    }

    @Test
    fun `execute with empty map returns NFCData with no card type`() {
        val result = interpretNfcDataUseCase.execute(emptyMap())

        assertNull(result.cardType)
        assertNull(result.applicationLabel)
        assertEquals(emptyMap<String, String>(), result.parsedTlvData)
    }

    @Test
    fun `execute resolves Visa AID from APPLICATION_IDENTIFIER_ADDITIONAL`() {
        val parsed = mapOf(
            EmvTag.APPLICATION_IDENTIFIER_ADDITIONAL.name to "A0000000031010"
        )

        val result = interpretNfcDataUseCase.execute(parsed)

        assertEquals("Visa", result.cardType)
    }

    @Test
    fun `execute resolves MasterCard AID from APPLICATION_IDENTIFIER`() {
        val parsed = mapOf(
            EmvTag.APPLICATION_IDENTIFIER.name to "A0000000041010"
        )

        val result = interpretNfcDataUseCase.execute(parsed)

        assertEquals("MasterCard", result.cardType)
    }

    @Test
    fun `execute decodes application label hex into UTF-8 string`() {
        // "VISA" in ASCII hex = 56 49 53 41
        val parsed = mapOf(EmvTag.APPLICATION_LABEL.name to "56495341")

        val result = interpretNfcDataUseCase.execute(parsed)

        assertEquals("VISA", result.applicationLabel)
    }

    @Test
    fun `execute decodes currency code EUR`() {
        // 0x0978 = 978 = EUR
        val parsed = mapOf(EmvTag.CURRENCY_CODE.name to "0978")

        val result = interpretNfcDataUseCase.execute(parsed)

        assertEquals("EUR", result.currencyCode)
    }

    @Test
    fun `execute preserves parsed TLV data on output`() {
        val parsed = mapOf(
            EmvTag.APPLICATION_PAN.name to "XXXXXXXXXXXX1111",
            EmvTag.EXPIRATION_DATE.name to "250228"
        )

        val result = interpretNfcDataUseCase.execute(parsed)

        assertEquals(parsed, result.parsedTlvData)
        assertNotNull(result.parsedTlvData[EmvTag.APPLICATION_PAN.name])
    }
}
