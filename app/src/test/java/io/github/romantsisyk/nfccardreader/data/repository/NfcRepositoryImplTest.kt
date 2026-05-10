package io.github.romantsisyk.nfccardreader.data.repository

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies that NfcRepositoryImpl never serialises sensitive EMV tags into
 * the JSON blob persisted to Room. We invoke the private `serializeTlvMap`
 * via reflection so we can exercise the filtering logic without spinning up
 * a Hilt graph or a real Room database. Robolectric is required so that
 * org.json.JSONObject runs against a real implementation rather than the
 * Android SDK stub.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NfcRepositoryImplTest {

    private fun newRepository(): NfcRepositoryImpl {
        // The serializer doesn't touch any of these collaborators, so mocks suffice.
        return NfcRepositoryImpl(
            context = mock(android.content.Context::class.java),
            processNfcIntentUseCase = mock(
                io.github.romantsisyk.nfccardreader.domain.usecase.ProcessNfcIntentUseCase::class.java
            ),
            scanDao = mock(
                io.github.romantsisyk.nfccardreader.data.local.dao.ScanDao::class.java
            )
        )
    }

    private fun callSerialize(repo: NfcRepositoryImpl, map: Map<String, String>): String {
        val method = NfcRepositoryImpl::class.java.getDeclaredMethod(
            "serializeTlvMap", Map::class.java
        )
        method.isAccessible = true
        return method.invoke(repo, map) as String
    }

    @Test
    fun `serializeTlvMap omits all sensitive tags`() {
        val repo = newRepository()
        val map = mapOf(
            "CARDHOLDER_NAME"        to "JOHN DOE",
            "EXPIRATION_DATE"        to "250228",
            "TRACK2_EQUIVALENT_DATA" to "XXXXXXXXXXXX1111D25022010000000000000",
            "Tag 5F34"               to "01",
            "Tag 9F1F"               to "DEADBEEF",
            "Tag 56"                 to "ABCD",
            "APPLICATION_LABEL"      to "VISA"
        )

        val json = JSONObject(callSerialize(repo, map))

        // Sensitive entries must be stripped.
        assertFalse("CARDHOLDER_NAME must not be persisted", json.has("CARDHOLDER_NAME"))
        assertFalse("EXPIRATION_DATE must not be persisted", json.has("EXPIRATION_DATE"))
        assertFalse("TRACK2_EQUIVALENT_DATA must not be persisted", json.has("TRACK2_EQUIVALENT_DATA"))
        assertFalse("PAN sequence (Tag 5F34) must not be persisted", json.has("Tag 5F34"))
        assertFalse("Track 1 discretionary (Tag 9F1F) must not be persisted", json.has("Tag 9F1F"))
        assertFalse("Issuer-specific (Tag 56) must not be persisted", json.has("Tag 56"))

        // Non-sensitive entries are preserved.
        assertTrue("APPLICATION_LABEL should be persisted", json.has("APPLICATION_LABEL"))
        assertEquals("VISA", json.getString("APPLICATION_LABEL"))
    }

    @Test
    fun `serializeTlvMap preserves non-sensitive entries verbatim`() {
        val repo = newRepository()
        val map = mapOf(
            "APPLICATION_IDENTIFIER" to "A0000000031010",
            "APPLICATION_LABEL"      to "VISA DEBIT",
            "DEDICATED_FILE_NAME"    to "325041592E5359532E4444463031"
        )

        val json = JSONObject(callSerialize(repo, map))

        assertEquals("A0000000031010", json.getString("APPLICATION_IDENTIFIER"))
        assertEquals("VISA DEBIT", json.getString("APPLICATION_LABEL"))
        assertEquals("325041592E5359532E4444463031", json.getString("DEDICATED_FILE_NAME"))
        assertEquals(3, json.length())
    }

    @Test
    fun `serializeTlvMap returns empty JSON object for empty map`() {
        val repo = newRepository()
        val json = JSONObject(callSerialize(repo, emptyMap()))
        assertEquals(0, json.length())
        assertNotNull(json)
    }
}
