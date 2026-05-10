package io.github.romantsisyk.nfccardreader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmvTagTest {

    @Test
    fun `test fromTag with known tag`() {
        // Given
        val knownTag = "5A" // APPLICATION_PAN

        // When
        val result = EmvTag.fromTag(knownTag)

        // Then
        assertEquals(EmvTag.APPLICATION_PAN, result)
    }

    @Test
    fun `test fromTag with 2-byte known tag`() {
        // Given
        val knownTag = "5F20" // CARDHOLDER_NAME

        // When
        val result = EmvTag.fromTag(knownTag)

        // Then
        assertEquals(EmvTag.CARDHOLDER_NAME, result)
    }

    @Test
    fun `test fromTag with unknown tag`() {
        // Given
        val unknownTag = "9999" // Not defined in EmvTag

        // When
        val result = EmvTag.fromTag(unknownTag)

        // Then
        assertEquals(EmvTag.UNKNOWN, result)
    }

    @Test
    fun `test fromTag with empty string`() {
        // Given
        val emptyTag = ""

        // When
        val result = EmvTag.fromTag(emptyTag)

        // Then
        assertEquals(EmvTag.UNKNOWN, result)
    }

    @Test
    fun `test getDescription with known tag`() {
        // Given
        val knownTag = "5A" // APPLICATION_PAN

        // When
        val result = EmvTag.getDescription(knownTag)

        // Then
        assertEquals("Primary Account Number", result)
    }

    @Test
    fun `test getDescription with unknown tag`() {
        // Given
        val unknownTag = "9999" // Not defined in EmvTag

        // When
        val result = EmvTag.getDescription(unknownTag)

        // Then
        assertEquals("Unknown Tag", result)
    }

    @Test
    fun `test all tags have non-empty descriptions`() {
        for (tag in EmvTag.entries) {
            assertNotNull("Tag ${tag.name} should have a description", tag.description)
            assertTrue("Tag ${tag.name} has an empty description", tag.description.isNotEmpty())
        }
    }

    @Test
    fun `test all tags have valid tag values`() {
        for (tag in EmvTag.entries.filter { it != EmvTag.UNKNOWN }) {
            assertTrue(
                "Tag ${tag.name} has an invalid tag value: ${tag.tag}",
                tag.tag.matches(Regex("^[0-9A-Fa-f]{2}([0-9A-Fa-f]{2})?$"))
            )
        }
    }

    @Test
    fun `test no duplicate tag values`() {
        val seen = mutableSetOf<String>()
        val duplicates = mutableListOf<String>()
        for (tag in EmvTag.entries.filter { it != EmvTag.UNKNOWN }) {
            if (!seen.add(tag.tag)) duplicates.add(tag.tag)
        }
        assertTrue("Found duplicate tag values: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `test common tags are defined`() {
        val commonTagValues = listOf(
            "5A", "5F20", "5F24", "9F02", "9F03", "9F06",
            "9F26", "95", "9F34", "82", "9F36", "9F37", "9F10", "9F1A"
        )
        for (tagValue in commonTagValues) {
            assertTrue(
                "Common tag $tagValue is not defined in EmvTag enum",
                EmvTag.fromTag(tagValue) != EmvTag.UNKNOWN
            )
        }
    }

    @Test
    fun `test cardholder name tag is well-defined`() {
        // Given/When
        val cardholderNameTag = EmvTag.CARDHOLDER_NAME

        // Then
        assertEquals("5F20", cardholderNameTag.tag)
        assertEquals("Cardholder Name", cardholderNameTag.description)
    }

    @Test
    fun `test application PAN tag is well-defined`() {
        // Given/When
        val panTag = EmvTag.APPLICATION_PAN

        // Then
        assertEquals("5A", panTag.tag)
        assertEquals("Primary Account Number", panTag.description)
    }

    @Test
    fun `test expiration date tag is well-defined`() {
        // Given/When
        val expirationDateTag = EmvTag.EXPIRATION_DATE

        // Then
        assertEquals("5F24", expirationDateTag.tag)
        assertEquals("Expiration Date", expirationDateTag.description)
    }
    
    @Test
    fun `test amount other tag is well-defined`() {
        // Given/When
        val amountOtherTag = EmvTag.AMOUNT_OTHER

        // Then
        assertEquals("9F03", amountOtherTag.tag)
        assertEquals("Amount, Other", amountOtherTag.description)
    }
    
    @Test
    fun `test application identifier additional tag is well-defined`() {
        // Given/When
        val aidTag = EmvTag.APPLICATION_IDENTIFIER_ADDITIONAL

        // Then
        assertEquals("9F06", aidTag.tag)
        assertEquals("Application Identifier (AID)", aidTag.description)
    }
}