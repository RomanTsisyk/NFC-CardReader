package io.github.romantsisyk.nfccardreader.domain.usecase

import io.github.romantsisyk.nfccardreader.domain.EmvTag
import io.github.romantsisyk.nfccardreader.utils.NfcDataMasker
import javax.inject.Inject

class ParseTLVUseCase @Inject constructor() {

    /**
     * Parses a BER-TLV encoded EMV APDU response.
     * Strips the SW1/SW2 status word, handles multi-byte tags,
     * long-form lengths, and recurses into constructed (template) tags.
     */
    fun execute(data: ByteArray): Map<String, String> {
        val payload = stripStatusWord(data)
        return parseTlv(payload, 0, payload.size)
    }

    private fun stripStatusWord(data: ByteArray): ByteArray {
        if (data.size >= 2
            && (data[data.size - 2].toInt() and 0xFF) == 0x90
            && (data[data.size - 1].toInt() and 0xFF) == 0x00) {
            return data.copyOf(data.size - 2)
        }
        return data
    }

    private fun parseTlv(data: ByteArray, start: Int, end: Int): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var index = start

        while (index < end) {

            // ── Skip BER-TLV padding bytes (0x00) between fields ──────────
            // EMV Book 3 §4.2 allows 0x00 as inter-TLV padding. 0xFF is NOT
            // skipped because it's a legal (private-use) tag start byte.
            val peek = data[index].toInt() and 0xFF
            if (peek == 0x00) { index++; continue }

            // ── Read tag ──────────────────────────────────────────────────
            if (index >= end) break
            val firstByte = data[index].toInt() and 0xFF
            val isConstructed = (firstByte and 0x20) != 0
            val tagBuilder = StringBuilder("%02X".format(firstByte))
            index++

            // Multi-byte tag: lower 5 bits all set means tag continues
            if ((firstByte and 0x1F) == 0x1F) {
                while (index < end) {
                    val b = data[index].toInt() and 0xFF
                    tagBuilder.append("%02X".format(b))
                    index++
                    if ((b and 0x80) == 0) break  // bit 8 = 0 → last tag byte
                }
            }
            val tag = tagBuilder.toString()

            // ── Read length ───────────────────────────────────────────────
            if (index >= end) break
            val firstLenByte = data[index].toInt() and 0xFF
            index++

            val length: Int = if ((firstLenByte and 0x80) == 0) {
                firstLenByte  // short form
            } else {
                val numBytes = firstLenByte and 0x7F
                if (numBytes == 0 || numBytes > 4 || index + numBytes > end) break
                var len = 0
                repeat(numBytes) { len = (len shl 8) or (data[index++].toInt() and 0xFF) }
                len
            }

            if (length < 0 || index + length > end) break

            val valueStart = index
            val valueEnd   = index + length
            index += length

            // ── Process value ─────────────────────────────────────────────
            if (isConstructed) {
                // Record the constructed template itself (raw hex) so callers
                // can see e.g. APPLICATION_TEMPLATE / FCI bytes, then recurse
                // into children (children win on key collision via putAll).
                val rawHex = data.sliceArray(valueStart until valueEnd)
                    .joinToString("") { "%02X".format(it) }
                when (val emvTag = EmvTag.fromTag(tag)) {
                    EmvTag.UNKNOWN -> result["Tag $tag"] = rawHex
                    else -> result[emvTag.name] = rawHex
                }
                result.putAll(parseTlv(data, valueStart, valueEnd))
            } else {
                val value = data.sliceArray(valueStart until valueEnd)
                when (val emvTag = EmvTag.fromTag(tag)) {
                    EmvTag.CARDHOLDER_NAME ->
                        result[emvTag.name] = value.toString(Charsets.UTF_8).trim()
                    EmvTag.APPLICATION_PAN -> {
                        val hex = value.joinToString("") { "%02X".format(it) }
                        result[emvTag.name] = NfcDataMasker.maskPan(hex)
                    }
                    EmvTag.TRACK2_EQUIVALENT_DATA -> {
                        val hex = value.joinToString("") { "%02X".format(it) }
                        result[emvTag.name] = NfcDataMasker.maskTrack2Data(hex)
                    }
                    EmvTag.APPLICATION_PREFERRED_NAME ->
                        result[emvTag.name] = value.toString(Charsets.UTF_8).trim()
                    EmvTag.UNKNOWN ->
                        result["Tag $tag"] = value.joinToString("") { "%02X".format(it) }
                    else ->
                        result[emvTag.name] = value.joinToString("") { "%02X".format(it) }
                }
            }
        }

        return result
    }
}
