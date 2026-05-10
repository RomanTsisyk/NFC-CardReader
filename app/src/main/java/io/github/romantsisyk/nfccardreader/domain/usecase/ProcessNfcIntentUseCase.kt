package io.github.romantsisyk.nfccardreader.domain.usecase

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
import io.github.romantsisyk.nfccardreader.utils.NfcDataDecoder
import javax.inject.Inject

class ProcessNfcIntentUseCase @Inject constructor(
    private val parseTLVUseCase: ParseTLVUseCase,
    private val interpretNfcDataUseCase: InterpretNfcDataUseCase
) {

    companion object {
        // PPSE: Proximity Payment System Environment (lists supported AIDs)
        private val PPSE_AID = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)

        // Common payment AIDs to try if PPSE fails or returns no usable AID
        private val FALLBACK_AIDS = listOf(
            byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x10, 0x10),        // Visa
            byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x10, 0x10),        // Mastercard
            byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x25, 0x01, 0x01),        // Amex
            byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x65, 0x10, 0x10),        // JCB
            byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x03, 0x24, 0x10, 0x01),        // UnionPay
            byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x01, 0x52, 0x30, 0x10),        // Discover
        )
    }

    @Suppress("DEPRECATION")
    fun execute(intent: Intent): NFCData {
        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        requireNotNull(tag) { "No NFC tag found in the intent" }

        val isoDep = IsoDep.get(tag)
            ?: throw UnsupportedOperationException("Card does not support ISO-DEP (IsoDep) protocol")

        isoDep.use { dep ->
            dep.connect()
            dep.timeout = 5000

            // 1. Try PPSE to discover the application on the card
            val aidToSelect = selectAidViaPpse(dep) ?: selectFirstWorkingAid(dep)
                ?: throw UnsupportedOperationException("No supported payment application found on card")

            // 2. SELECT the chosen AID (returns FCI of the application)
            val fciResponse = selectAid(dep, aidToSelect)

            // 3. Issue GET PROCESSING OPTIONS to obtain AIP + AFL
            //    Concatenate the GPO response (best-effort) with the FCI so
            //    downstream parsing sees both the application FCI tags and any
            //    GPO/READ RECORD tags. Failures here are non-fatal — many card
            //    profiles still expose useful tags via the FCI alone.
            val combined = mutableListOf<Byte>()
            combined.addAll(fciResponse.toList())

            val gpoResponse = runCatching { sendGpo(dep, fciResponse) }.getOrNull()
            if (gpoResponse != null) {
                combined.addAll(gpoResponse.toList())

                // 4. If GPO returned an AFL (tag 94), READ RECORD each entry
                val gpoParsed = runCatching {
                    parseTLVUseCase.execute(gpoResponse)
                }.getOrDefault(emptyMap())
                val aflHex = gpoParsed["APPLICATION_FILE_LOCATOR"] ?: gpoParsed["Tag 94"]
                if (aflHex != null) {
                    runCatching { readAllRecords(dep, aflHex) }
                        .getOrDefault(emptyList<ByteArray>())
                        .forEach { combined.addAll(it.toList()) }
                }
            }

            val parsed = parseTLVUseCase.execute(combined.toByteArray())
            return interpretNfcDataUseCase.execute(parsed)
        }
    }

    /** Returns the first AID found in the PPSE response, or null if PPSE is unsupported. */
    private fun selectAidViaPpse(dep: IsoDep): ByteArray? {
        return try {
            val ppseRaw = transceiveWithGetResponse(dep, buildSelectCommand(PPSE_AID))
            if (!isSw9000(ppseRaw)) return null

            // PPSE FCI nesting: 6F → A5 → BF0C → 61 (Application Template) → 4F (AID)
            // ParseTLVUseCase recurses into constructed templates, so 4F bubbles up.
            // We deliberately extract from the inner Application Template (61) when
            // available, since BF0C may list multiple 61 templates with priority (87).
            val parsed = parseTLVUseCase.execute(ppseRaw)
            val aidHex = parsed["APPLICATION_IDENTIFIER"] ?: return null
            hexToBytes(aidHex)
        } catch (e: Exception) {
            null
        }
    }

    /** Tries each fallback AID; returns the first one the card accepts. */
    private fun selectFirstWorkingAid(dep: IsoDep): ByteArray? {
        for (aid in FALLBACK_AIDS) {
            try {
                val response = transceiveWithGetResponse(dep, buildSelectCommand(aid))
                if (isSw9000(response)) return aid
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    /** Selects an AID and returns the full FCI response (including nested TLV). */
    private fun selectAid(dep: IsoDep, aid: ByteArray): ByteArray {
        val response = transceiveWithGetResponse(dep, buildSelectCommand(aid))
        if (!isSw9000(response)) {
            throw IllegalStateException(
                "Card returned error selecting AID: ${describeStatusWord(response)}"
            )
        }
        return response
    }

    /**
     * Sends GET PROCESSING OPTIONS (GPO).
     * Builds the PDOL-conditioned data field by reading PDOL (tag 9F38) from
     * the FCI; if the card requested no PDOL data, sends an empty list (83 00).
     * Returns the full GPO response bytes (including SW1/SW2). Throws on error.
     */
    private fun sendGpo(dep: IsoDep, fciResponse: ByteArray): ByteArray {
        val pdolValue = extractPdolDefault(fciResponse)
        // Command Template tag 83 wraps the concatenated PDOL value
        val cmdTemplate = byteArrayOf(0x83.toByte(), pdolValue.size.toByte()) + pdolValue
        val gpoCommand = byteArrayOf(
            0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, cmdTemplate.size.toByte()
        ) + cmdTemplate + byteArrayOf(0x00)
        val response = transceiveWithGetResponse(dep, gpoCommand)
        if (!isSw9000(response)) {
            throw IllegalStateException(
                "GPO failed: ${describeStatusWord(response)}"
            )
        }
        return response
    }

    /**
     * Walks the AFL (Application File Locator) entries and issues READ RECORD
     * for each. AFL is a sequence of 4-byte entries: SFI(5 high bits)|00, first
     * record, last record, # records involved in offline auth.
     * Returns the concatenated record responses.
     */
    private fun readAllRecords(dep: IsoDep, aflHex: String): List<ByteArray> {
        val afl = hexToBytes(aflHex)
        if (afl.size < 4 || afl.size % 4 != 0) return emptyList()
        val out = mutableListOf<ByteArray>()
        var i = 0
        while (i + 3 < afl.size) {
            val sfi = (afl[i].toInt() and 0xFF) ushr 3
            val firstRec = afl[i + 1].toInt() and 0xFF
            val lastRec = afl[i + 2].toInt() and 0xFF
            for (rec in firstRec..lastRec) {
                val cmd = byteArrayOf(
                    0x00,
                    0xB2.toByte(),                        // READ RECORD
                    rec.toByte(),
                    ((sfi shl 3) or 0x04).toByte(),       // P2: SFI<<3 | 0b100
                    0x00
                )
                runCatching {
                    val resp = transceiveWithGetResponse(dep, cmd)
                    if (isSw9000(resp)) out.add(resp)
                }
            }
            i += 4
        }
        return out
    }

    /**
     * Sends an APDU and transparently follows up with GET RESPONSE / Le-retry
     * if the card replies with 0x61xx (more data) or 0x6Cxx (wrong Le).
     */
    private fun transceiveWithGetResponse(dep: IsoDep, command: ByteArray): ByteArray {
        var current = dep.transceive(command)
        var guard = 0
        while (current.size >= 2 && guard < 8) {
            val sw1 = current[current.size - 2].toInt() and 0xFF
            val sw2 = current[current.size - 1].toInt() and 0xFF
            when (sw1) {
                0x61 -> {
                    // More data — issue GET RESPONSE to drain it, append data
                    val getResp = byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, sw2.toByte())
                    val next = dep.transceive(getResp)
                    val merged = current.copyOf(current.size - 2) + next
                    current = merged
                }
                0x6C -> {
                    // Wrong Le — retry the previous command with the correct Le
                    val retried = command.copyOf()
                    retried[retried.size - 1] = sw2.toByte()
                    current = dep.transceive(retried)
                }
                else -> return current
            }
            guard++
        }
        return current
    }

    /**
     * Reads PDOL (tag 9F38) from the FCI response and returns a byte string of
     * default values (zero-filled) sized per PDOL's TL pairs. If no PDOL is
     * present, returns an empty array (caller sends 83 00).
     */
    private fun extractPdolDefault(fci: ByteArray): ByteArray {
        val parsed = runCatching { parseTLVUseCase.execute(fci) }.getOrNull() ?: return ByteArray(0)
        val pdolHex = parsed["PDOL"] ?: parsed["Tag 9F38"] ?: return ByteArray(0)
        val pdol = hexToBytes(pdolHex)
        val out = mutableListOf<Byte>()
        var i = 0
        while (i < pdol.size) {
            // Skip tag (handle multi-byte: lower 5 bits all 1 means continuation)
            val first = pdol[i].toInt() and 0xFF
            i++
            if ((first and 0x1F) == 0x1F) {
                while (i < pdol.size) {
                    val b = pdol[i].toInt() and 0xFF
                    i++
                    if ((b and 0x80) == 0) break
                }
            }
            if (i >= pdol.size) break
            val len = pdol[i].toInt() and 0xFF
            i++
            repeat(len) { out.add(0x00) }
        }
        return out.toByteArray()
    }

    private fun buildSelectCommand(aid: ByteArray): ByteArray =
        byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid + byteArrayOf(0x00)

    private fun isSw9000(response: ByteArray): Boolean =
        response.size >= 2
            && (response[response.size - 2].toInt() and 0xFF) == 0x90
            && (response[response.size - 1].toInt() and 0xFF) == 0x00

    private fun describeStatusWord(response: ByteArray): String {
        if (response.size < 2) return "no response"
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return "SW=%02X%02X (%s)".format(sw1, sw2, NfcDataDecoder.decodeStatusWord(sw1, sw2))
    }

    private fun hexToBytes(hex: String): ByteArray =
        hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
