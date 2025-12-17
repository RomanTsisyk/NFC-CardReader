package io.github.romantsisyk.nfccardreader.domain.usecase

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
import javax.inject.Inject

/**
 * Use case for processing NFC intents and extracting card data.
 *
 * This use case handles the low-level NFC communication with payment cards,
 * parsing TLV data and interpreting EMV tags.
 *
 * @property parseTLVUseCase Use case for parsing TLV byte arrays
 * @property interpretNfcDataUseCase Use case for interpreting parsed NFC data
 */
open class ProcessNfcIntentUseCase @Inject constructor(
    private val parseTLVUseCase: ParseTLVUseCase?,
    val interpretNfcDataUseCase: InterpretNfcDataUseCase?
) {

    /**
     * Executes NFC intent processing and returns parsed card data.
     *
     * @param intent The NFC intent containing tag information
     * @return NFCData containing all parsed card information
     * @throws IllegalArgumentException if no NFC tag found in intent
     * @throws UnsupportedOperationException if tag doesn't support IsoDep
     * @throws IllegalStateException if required dependencies are missing
     */
    @Suppress("DEPRECATION")
    open fun execute(intent: Intent): NFCData {
        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag == null) {
            throw IllegalArgumentException("No NFC tag found in the intent")
        }

        val isoDep = IsoDep.get(tag) ?: throw UnsupportedOperationException("Unsupported NFC Tag")

        isoDep.use { dep ->
            dep.connect()
            val selectVisaCommand = byteArrayOf(
                0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
                0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x03.toByte(),
                0x10.toByte(), 0x10.toByte(), 0x00.toByte()
            )
            val response = dep.transceive(selectVisaCommand)

            // Create a readable hex string for rawResponse
            val rawResponse = response.joinToString(" ") { "%02X".format(it) }

            // Check if the dependencies are available (for testing)
            if (parseTLVUseCase == null || interpretNfcDataUseCase == null) {
                throw IllegalStateException("Missing dependencies in ProcessNfcIntentUseCase")
            }

            val parsedTlvData = parseTLVUseCase.execute(response)
            return interpretNfcDataUseCase.execute(response).copy(
                parsedTlvData = parsedTlvData,
                rawResponse = rawResponse
            )
        }
    }
}
