package io.github.romantsisyk.nfccardreader.utils

import org.junit.Assert.*
import org.junit.Test

class NfcDataMaskerTest {

    @Test
    fun `test maskPan with standard length PAN`() {
        // Given
        val pan = "4111111111111111"
        
        // When
        val result = NfcDataMasker.maskPan(pan)
        
        // Then
        assertTrue(result.endsWith("1111"))
        assertTrue(result.contains("X"))
        assertEquals(16, result.length)
    }

    @Test
    fun `test maskPan with short PAN`() {
        // Given
        val pan = "411111"
        
        // When
        val result = NfcDataMasker.maskPan(pan)
        
        // Then
        assertTrue(result.endsWith("1111"))
        assertEquals(6, result.length)
    }

    @Test
    fun `test maskPan with PAN less than 4 digits`() {
        // Given
        val pan = "123"
        
        // When
        val result = NfcDataMasker.maskPan(pan)
        
        // Then
        assertEquals("123", result)
    }

    @Test
    fun `test maskPan with empty string`() {
        // Given
        val pan = ""
        
        // When
        val result = NfcDataMasker.maskPan(pan)
        
        // Then
        assertEquals("", result)
    }

    @Test
    fun `test maskTrack2Data with standard track2 data`() {
        // Given
        val track2 = "4111111111111111=25022010000000000000"
        
        // When
        val result = NfcDataMasker.maskTrack2Data(track2)
        
        // Then
        assertTrue(result.contains("X"))
        assertEquals(track2.length, result.length)
    }

    @Test
    fun `test maskTrack2Data with HEX track2 data`() {
        // Given
        val track2 = "4111111111111111D25022010000000000000"
        
        // When
        val result = NfcDataMasker.maskTrack2Data(track2)
        
        // Then
        assertTrue(result.contains("X"))
        assertTrue(result.contains("D"))
    }

    @Test
    fun `test maskTrack2Data with empty string`() {
        // Given
        val track2 = ""

        // When
        val result = NfcDataMasker.maskTrack2Data(track2)

        // Then
        assertEquals("", result)
    }

    @Test
    fun `maskPan strips BCD pad nibble for 15-digit Amex PAN`() {
        // 15-digit Amex PAN encoded in EMV BCD with trailing F nibble pad.
        val amexBcd = "378282246310005F"

        val result = NfcDataMasker.maskPan(amexBcd)

        // Padding nibble must be removed before masking, and exactly the last
        // 4 digits of the real PAN must remain visible.
        assertEquals("XXXXXXXXXXX0005", result)
        assertEquals(15, result.length)
        assertTrue("Must not leak any of the leading PAN digits", !result.contains("3782"))
    }

    @Test
    fun `maskPan with trailing lowercase f pad is also stripped`() {
        val amexBcd = "378282246310005f"

        val result = NfcDataMasker.maskPan(amexBcd)

        assertEquals("XXXXXXXXXXX0005", result)
    }
}