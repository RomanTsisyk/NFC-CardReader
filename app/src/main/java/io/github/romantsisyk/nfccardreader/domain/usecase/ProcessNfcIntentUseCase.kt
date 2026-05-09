package io.github.romantsisyk.nfccardreader.domain.usecase

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
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

            // 2. SELECT the chosen AID
            val fciResponse = selectAid(dep, aidToSelect)

            // 3. Parse the FCI response + run GET PROCESSING OPTIONS (GPO) if available
            val parsed = parseTLVUseCase.execute(fciResponse)

            return interpretNfcDataUseCase.execute(parsed)
        }
    }

    /** Returns the first AID found in the PPSE response, or null if PPSE is unsupported. */
    private fun selectAidViaPpse(dep: IsoDep): ByteArray? {
        return try {
            val ppseResponse = dep.transceive(buildSelectCommand(PPSE_AID))
            if (!isSw9000(ppseResponse)) return null

            // Parse PPSE FCI; AIDs are in constructed BF0C > 61 > 4F
            val parsed = parseTLVUseCase.execute(ppseResponse)
            val aidHex = parsed["APPLICATION_IDENTIFIER"] ?: return null
            aidHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    /** Tries each fallback AID; returns the first one the card accepts. */
    private fun selectFirstWorkingAid(dep: IsoDep): ByteArray? {
        for (aid in FALLBACK_AIDS) {
            try {
                val response = dep.transceive(buildSelectCommand(aid))
                if (isSw9000(response)) return aid
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    /** Selects an AID and returns the full FCI response (including nested TLV). */
    private fun selectAid(dep: IsoDep, aid: ByteArray): ByteArray {
        val response = dep.transceive(buildSelectCommand(aid))
        if (!isSw9000(response)) {
            val sw = response.takeLast(2).joinToString("") { "%02X".format(it) }
            throw IllegalStateException("Card returned error selecting AID: SW=$sw")
        }
        return response
    }

    private fun buildSelectCommand(aid: ByteArray): ByteArray =
        byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid + byteArrayOf(0x00)

    private fun isSw9000(response: ByteArray): Boolean =
        response.size >= 2
            && (response[response.size - 2].toInt() and 0xFF) == 0x90
            && (response[response.size - 1].toInt() and 0xFF) == 0x00
}
