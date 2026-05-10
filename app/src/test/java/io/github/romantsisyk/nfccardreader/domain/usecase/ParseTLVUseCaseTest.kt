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
        // Tag 01 is single-byte primitive UNKNOWN (not in EmvTag enum)
        val data = createByteArrayFromHex("01 02 AA BB")
        val result = parseTLVUseCase.execute(data)

        assertTrue("Unknown tag should be stored with 'Tag' prefix",
            result.keys.any { it.startsWith("Tag") })
    }

    @Test
    fun `parses real Visa PPSE response and extracts AID`() {
        // Visa PPSE FCI response. Structure:
        //   6F (FCI Template)
        //     84 0E "2PAY.SYS.DDF01"             (DF Name)
        //     A5 (FCI Proprietary)
        //       BF 0C (FCI Issuer Discretionary Data)
        //         61 (Application Template)
        //           4F 07 A0000000031010          (AID = Visa)
        //           50 0A "VISA DEBIT"            (Application Label)
        // Lengths are recomputed for byte-correctness.
        // Inner 61: 4F(1)+07(1)+7 + 50(1)+0A(1)+10 = 21 -> 0x15
        // BF0C: 61(1)+15(1)+21 = 23 -> 0x17
        // A5:   BF(1)+0C(1)+17(1)+23 = 26 -> 0x1A
        // 6F:   84(1)+0E(1)+14 + A5(1)+1A(1)+26 = 44 -> 0x2C
        val ppse = "6F2C" +
            "840E325041592E5359532E4444463031" +     // DF Name
            "A51A" +
            "BF0C17" +
            "6115" +
            "4F07A0000000031010" +                    // Visa AID
            "500A56495341204445424954"                 // "VISA DEBIT"
        val data = createByteArrayFromHex(ppse)

        val result = parseTLVUseCase.execute(data)

        // Constructed templates 6F, A5, BF0C, 61 are recursed; primitives are merged in.
        // 4F = APPLICATION_IDENTIFIER, stored as hex.
        assertEquals(
            "Visa AID should be extracted from PPSE",
            "A0000000031010",
            result["APPLICATION_IDENTIFIER"]
        )
        // 84 = DEDICATED_FILE_NAME (primitive), stored as hex.
        assertTrue(
            "DF Name should be present",
            result.containsKey("DEDICATED_FILE_NAME")
        )
        // 50 = APPLICATION_LABEL (primitive), stored as hex per ParseTLVUseCase else branch.
        assertEquals(
            "56495341204445424954",
            result["APPLICATION_LABEL"]
        )
    }

    @Test
    fun `parses constructed FCI template with 84 and A5 children`() {
        // 6F (FCI Template) containing:
        //   84 07 A0000000031010   (DF Name)
        //   A5 03 50 01 41         (FCI Proprietary -> APPLICATION_LABEL "A")
        // 6F payload = 84(1)+07(1)+7 + A5(1)+03(1)+3 = 14 -> 0x0E
        val data = createByteArrayFromHex(
            "6F0E" +
                "8407A0000000031010" +
                "A503500141"
        )

        val result = parseTLVUseCase.execute(data)

        assertEquals("A0000000031010", result["DEDICATED_FILE_NAME"])
        assertEquals("41", result["APPLICATION_LABEL"])
    }

    @Test
    fun `handles long-form length 0x81 with length byte greater than 127`() {
        // Primitive tag 9F10 (ISSUER_APPLICATION_DATA) with long-form length 0x81 0x80
        // -> 128 bytes of value. Verifies the parser correctly reads multi-byte tag,
        // honours long-form length, and consumes the full 128-byte value.
        val valueHex = "AB".repeat(128)
        val data = createByteArrayFromHex("9F10" + "8180" + valueHex)

        val result = parseTLVUseCase.execute(data)

        assertEquals(
            "Long-form length 0x81 0x80 must read full 128-byte value",
            valueHex,
            result["ISSUER_APPLICATION_DATA"]
        )
    }
}
