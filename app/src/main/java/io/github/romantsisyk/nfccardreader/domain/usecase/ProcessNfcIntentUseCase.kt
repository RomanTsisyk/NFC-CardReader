package io.github.romantsisyk.nfccardreader.domain.usecase

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
import javax.inject.Inject

class ProcessNfcIntentUseCase @Inject constructor(
    private val parseTLVUseCase: ParseTLVUseCase,
    val interpretNfcDataUseCase: InterpretNfcDataUseCase
) {

    fun execute(intent: Intent): NFCData {
        val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            ?: throw IllegalArgumentException("No NFC tag found in the intent")

        val isoDep = IsoDep.get(tag) ?: throw UnsupportedOperationException("Unsupported NFC Tag")

        isoDep.use { isoDep ->
            isoDep.connect()
            val selectVisaCommand = byteArrayOf(
                0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
                0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x03.toByte(),
                0x10.toByte(), 0x10.toByte(), 0x00.toByte()
            )
            val response = isoDep.transceive(selectVisaCommand)

            // Create a readable hex string for rawResponse
            val rawResponse = response.joinToString(" ") { "%02X".format(it) }

            val parsedTlvData = parseTLVUseCase.execute(response)
            return interpretNfcDataUseCase.execute(response).copy(
                parsedTlvData = parsedTlvData,
                rawResponse = rawResponse
            )
        }
    }
}
