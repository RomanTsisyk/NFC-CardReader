package io.github.romantsisyk.nfccardreader.domain.usecase

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
import javax.inject.Inject

open class ProcessNfcIntentUseCase @Inject constructor(
    private val parseTLVUseCase: ParseTLVUseCase?,
    val interpretNfcDataUseCase: InterpretNfcDataUseCase?
) {
    private val TAG = "ProcessNfcIntentUseCase"

    open fun execute(intent: Intent): NFCData {
        val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            ?: throw IllegalArgumentException("No NFC tag found in the intent")

        val isoDep = IsoDep.get(tag) ?: throw UnsupportedOperationException("Unsupported NFC Tag")

        try {
            // Increased timeout to give more time for NFC operations
            isoDep.timeout = 5000
            if (!isoDep.isConnected) {
                isoDep.connect()
            }

            // Check if the tag is still in the field
            if (!isoDep.isConnected) {
                throw IllegalStateException("Failed to connect to the NFC tag")
            }
            val selectVisaCommand = byteArrayOf(
                0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
                0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x03.toByte(),
                0x10.toByte(), 0x10.toByte(), 0x00.toByte()
            )
            val response = isoDep.transceive(selectVisaCommand)

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
        } catch (e: Exception) {
            Log.e(TAG, "Error processing NFC tag: ${e.message}", e)
            throw e
        } finally {
            try {
                if (isoDep.isConnected) {
                    isoDep.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error closing IsoDep connection: ${e.message}", e)
            }
        }
    }
}
