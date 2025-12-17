package io.github.romantsisyk.nfccardreader.domain.usecase

import android.util.Log
import io.github.romantsisyk.nfccardreader.domain.EmvTag
import io.github.romantsisyk.nfccardreader.utils.NfcDataMasker

/**
 * Use case for parsing TLV (Tag-Length-Value) encoded data from NFC responses.
 *
 * This class handles the parsing of EMV-formatted TLV data commonly found
 * in payment card NFC communications. It extracts and decodes various
 * tags including cardholder name, PAN (masked for security), and expiration date.
 *
 * @see EmvTag for supported EMV tags
 * @see NfcDataMasker for PAN masking logic
 */
class ParseTLVUseCase {

    private val TAG = "ParseTLVUseCase"

    /**
     * Parses TLV-encoded byte array into a map of tag names to values.
     *
     * The parsing follows the EMV TLV structure where each data element
     * consists of a tag, length, and value. Sensitive data like PANs
     * are automatically masked for PCI-DSS compliance.
     *
     * @param data The raw byte array containing TLV-encoded data
     * @return Map of tag descriptions to their decoded/masked values
     */
    fun execute(data: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var index = 0

        Log.d(TAG, "Starting TLV parsing. Data size: ${data.size}")

        while (index < data.size) {
            val tag = data[index++].toInt().and(0xFF).toString(16).padStart(2, '0').uppercase()
            Log.d(TAG, "Found tag: $tag at index $index")

            if (index >= data.size) {
                Log.w(TAG, "Index out of bounds after reading tag.")
                break
            }

            val length = data[index++].toInt().and(0xFF)
            Log.d(TAG, "Length of tag $tag: $length")

            if (index + length > data.size) {
                Log.w(TAG, "Not enough data remaining for tag $tag. Breaking.")
                break
            }

            val value = data.sliceArray(index until index + length)
            index += length
            Log.d(TAG, "Value for tag $tag: ${value.joinToString("") { "%02X".format(it) }}")

            // Map tags based on EMV tag definitions
            when (val emvTag = EmvTag.fromTag(tag)) {
                EmvTag.CARDHOLDER_NAME -> result[emvTag.name] = value.toString(Charsets.UTF_8)
                EmvTag.APPLICATION_PAN -> result[emvTag.name] = NfcDataMasker.maskPan(
                    value.joinToString("") { "%02X".format(it) }
                )
                EmvTag.TRACK2_EQUIVALENT_DATA -> result[emvTag.name] = NfcDataMasker.maskTrack2Data(
                    value.joinToString("") { "%02X".format(it) }
                )
                EmvTag.EXPIRATION_DATE -> result[emvTag.name] = value.joinToString("") { "%02X".format(it) }
                EmvTag.APPLICATION_PREFERRED_NAME -> result[emvTag.name] = value.toString(Charsets.UTF_8)
                EmvTag.UNKNOWN -> result["Tag $tag"] = value.joinToString("") { "%02X".format(it) }
                else -> {
                    val unparsedValue = value.joinToString("") { "%02X".format(it) }
                    result["Unparsed Tag $tag"] = unparsedValue
                    Log.d(TAG, "Unhandled tag $tag with value: $unparsedValue")
                }
            }
        }

        Log.d(TAG, "Completed TLV parsing. Parsed ${result.size} tags.")
        return result
    }
}
