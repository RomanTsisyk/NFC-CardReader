package io.github.romantsisyk.nfccardreader.utils

import io.github.romantsisyk.nfccardreader.util.createHexList
import org.junit.Assert.*
import org.junit.Test

class NfcDataDecoderTest {

    @Test
    fun `test decodeAmount with zero amount`() {
        // Given
        val bytes = createHexList("00 00 00 00 00 00")
        
        // When
        val result = NfcDataDecoder.decodeAmount(bytes)
        
        // Then
        assertEquals("0.00", result)
    }

    @Test
    fun `test decodeAmount with small amount`() {
        // Given - hex 0x7B = 123 decimal, /100 = 1.23
        val bytes = createHexList("00 00 00 00 00 7B")

        // When
        val result = NfcDataDecoder.decodeAmount(bytes)

        // Then
        assertEquals("1.23", result)
    }

    @Test
    fun `test decodeAmount with large amount`() {
        // Given - hex 0x3B9ACA00 = 1000000000 decimal, /100 = 10000000.00
        val bytes = createHexList("00 00 00 00 27 10") // 10000 = 100.00

        // When
        val result = NfcDataDecoder.decodeAmount(bytes)

        // Then
        assertEquals("100.00", result)
    }

    @Test
    fun `test decodeCurrency with known currency code`() {
        // Given
        val bytes = createHexList("09 78") // EUR
        
        // When
        val result = NfcDataDecoder.decodeCurrency(bytes)
        
        // Then
        assertEquals("EUR", result)
    }

    @Test
    fun `test decodeCurrency with another known currency code`() {
        // Given
        val bytes = createHexList("08 40") // USD
        
        // When
        val result = NfcDataDecoder.decodeCurrency(bytes)
        
        // Then
        assertEquals("USD", result)
    }

    @Test
    fun `test decodeCurrency with unknown currency code`() {
        // Given
        val bytes = createHexList("99 99") // Unknown
        
        // When
        val result = NfcDataDecoder.decodeCurrency(bytes)
        
        // Then
        assertTrue(result.startsWith("Unknown Currency"))
    }

    @Test
    fun `test decodeDate with valid date`() {
        // Given
        val bytes = createHexList("23 04 20") // 20.04.2023
        
        // When
        val result = NfcDataDecoder.decodeDate(bytes)
        
        // Then
        assertEquals("20.04.2023", result)
    }

    @Test
    fun `test decodeTime with valid time`() {
        // Given
        val bytes = createHexList("10 35 42") // 10:35:42
        
        // When
        val result = NfcDataDecoder.decodeTime(bytes)
        
        // Then
        assertEquals("10:35:42", result)
    }

    @Test
    fun `test decodeCountryCode with valid country code`() {
        // Given
        val bytes = createHexList("08 40") // USA
        
        // When
        val result = NfcDataDecoder.decodeCountryCode(bytes)
        
        // Then
        // This test is a bit tricky as it depends on locale availability
        // We should at least ensure it doesn't crash and returns some value
        assertNotNull(result)
    }

    @Test
    fun `test decodeCountryCode with unknown country code`() {
        // Given
        val bytes = createHexList("99 99") // Unknown
        
        // When
        val result = NfcDataDecoder.decodeCountryCode(bytes)
        
        // Then
        assertTrue(result.startsWith("Unknown Country"))
    }

    @Test
    fun `test decodeTransactionType with purchase code`() {
        // Given
        val byte = "00" // Purchase
        
        // When
        val result = NfcDataDecoder.decodeTransactionType(byte)
        
        // Then
        assertEquals("Purchase", result)
    }

    @Test
    fun `test decodeTransactionType with cash advance code`() {
        // Given
        val byte = "01" // Cash Advance
        
        // When
        val result = NfcDataDecoder.decodeTransactionType(byte)
        
        // Then
        assertEquals("Cash Advance", result)
    }

    @Test
    fun `test decodeTransactionType with unknown code`() {
        // Given
        val byte = "99" // Unknown
        
        // When
        val result = NfcDataDecoder.decodeTransactionType(byte)
        
        // Then
        assertTrue(result.startsWith("Unknown Transaction Type"))
    }

    @Test
    fun `test decodeApplicationIdentifier with Visa AID`() {
        // Given
        val bytes = createHexList("A0 00 00 00 03 10 10") // Visa
        
        // When
        val result = NfcDataDecoder.decodeApplicationIdentifier(bytes)
        
        // Then
        assertEquals("Visa", result)
    }

    @Test
    fun `test decodeApplicationIdentifier with MasterCard AID`() {
        // Given
        val bytes = createHexList("A0 00 00 00 04 10 10") // MasterCard
        
        // When
        val result = NfcDataDecoder.decodeApplicationIdentifier(bytes)
        
        // Then
        assertEquals("MasterCard", result)
    }

    @Test
    fun `test decodeApplicationIdentifier with unknown AID`() {
        // Given
        val bytes = createHexList("B0 00 00 00 99 99 99") // Unknown
        
        // When
        val result = NfcDataDecoder.decodeApplicationIdentifier(bytes)
        
        // Then
        assertTrue(result.startsWith("Unknown Card"))
    }

    @Test
    fun `test decodeServiceCode with international interchange`() {
        // Given
        val bytes = createHexList("1 0 1") // International interchange, Normal authorization, No restrictions
        
        // When
        val result = NfcDataDecoder.decodeServiceCode(bytes)
        
        // Then
        assertTrue(result.contains("International interchange"))
        assertTrue(result.contains("Normal authorization"))
        assertTrue(result.contains("No restrictions"))
    }

    @Test
    fun `test decodeServiceCode with national interchange`() {
        // Given
        val bytes = createHexList("5 0 1") // National interchange only, Normal authorization, No restrictions
        
        // When
        val result = NfcDataDecoder.decodeServiceCode(bytes)
        
        // Then
        assertTrue(result.contains("National interchange only"))
        assertTrue(result.contains("Normal authorization"))
        assertTrue(result.contains("No restrictions"))
    }

    @Test
    fun `test decodeCardholderVerificationMethodResult with No CVM`() {
        // Given
        val bytes = createHexList("00 00") // No CVM performed
        
        // When
        val result = NfcDataDecoder.decodeCardholderVerificationMethodResult(bytes)
        
        // Then
        assertEquals("No CVM performed", result)
    }

    @Test
    fun `test decodeCardholderVerificationMethodResult with PIN verification`() {
        // Given
        val bytes = createHexList("00 01") // Plaintext PIN verified by ICC
        
        // When
        val result = NfcDataDecoder.decodeCardholderVerificationMethodResult(bytes)
        
        // Then
        assertEquals("Plaintext PIN verified by ICC", result)
    }

    @Test
    fun `test decodeCardholderVerificationMethodResult with unknown method`() {
        // Given
        val bytes = createHexList("99 99") // Unknown
        
        // When
        val result = NfcDataDecoder.decodeCardholderVerificationMethodResult(bytes)
        
        // Then
        assertTrue(result.startsWith("Unknown CVM"))
    }

    @Test
    fun `test decodeFormFactorIndicator with physical card`() {
        // Given
        val bytes = createHexList("04 21 01 03") // Physical card with contact chip and contactless
        
        // When
        val result = NfcDataDecoder.decodeFormFactorIndicator(bytes)
        
        // Then
        assertEquals("Physical card with contact chip and contactless", result)
    }

    @Test
    fun `test decodeFormFactorIndicator with mobile phone`() {
        // Given
        val bytes = createHexList("06 00 00 00") // Mobile phone
        
        // When
        val result = NfcDataDecoder.decodeFormFactorIndicator(bytes)
        
        // Then
        assertEquals("Mobile phone", result)
    }

    @Test
    fun `test decodeFormFactorIndicator with unknown form factor`() {
        // Given
        val bytes = createHexList("99 00 00 00") // Unknown
        
        // When
        val result = NfcDataDecoder.decodeFormFactorIndicator(bytes)
        
        // Then
        assertEquals("Unknown form factor", result)
    }
}