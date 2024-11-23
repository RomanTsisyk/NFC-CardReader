package io.github.romantsisyk.nfccardreader

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NFCReaderViewModel : ViewModel() {

    private val _nfcTagData = MutableStateFlow<Map<String, String>>(emptyMap())
    val nfcTagData: StateFlow<Map<String, String>> = _nfcTagData

    private val _rawResponse = MutableStateFlow("")
    val rawResponse: StateFlow<String> = _rawResponse

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun processNfcIntent(intent: Intent) {
        val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        if (tag != null) {
            processTag(tag)
        } else {
            _error.value = "No NFC tag found in the intent"
        }
    }

    private fun processTag(tag: Tag) {
        try {
            val isoDep = IsoDep.get(tag)
            if (isoDep != null) {
                isoDep.connect()
                val selectVisaCommand = byteArrayOf(
                    0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
                    0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x03.toByte(), 0x10.toByte(), 0x10.toByte(), 0x00.toByte()
                )

                val response = isoDep.transceive(selectVisaCommand)
                _rawResponse.value = parseNfcResponse(response)

                Log.d("NFCReader", "APDU Command Sent: ${selectVisaCommand.joinToString(", ") { "%02X".format(it) }}")
                Log.d("NFCReader", "Raw Response: ${response.joinToString(", ") { "%02X".format(it) }}")

                val parsedData = parseTLV(response)
                _nfcTagData.value = parsedData
            } else {
                _error.value = "Unsupported NFC Tag"
            }
        } catch (e: Exception) {
            _error.value = "Error reading tag: ${e.message}"
        }
    }

    private fun parseTLV(data: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var index = 0

        while (index < data.size) {
            val tag = data[index++].toInt().and(0xFF).toString(16).padStart(2, '0').uppercase()
            if (index >= data.size) break
            val length = data[index++].toInt().and(0xFF)
            if (index + length > data.size) break
            val value = data.sliceArray(index until index + length)
            index += length

            when (tag) {
                "5F20" -> result["Cardholder Name"] = value.toString(Charsets.UTF_8)
                "5A" -> result["Application PAN"] = value.joinToString("") { "%02X".format(it) }
                "57" -> result["Track 2 Equivalent Data"] = value.joinToString("") { "%02X".format(it) }
                "5F24" -> result["Expiration Date"] = value.joinToString("") { "%02X".format(it) }
                "9F12" -> result["Application Preferred Name"] = value.toString(Charsets.UTF_8)
                else -> result["Tag $tag"] = value.joinToString("") { "%02X".format(it) }
            }
        }
        return result
    }

    private fun parseNfcResponse(response: ByteArray): String {
        return try {
            if (response.isNotEmpty()) {
                val asciiRepresentation = response.joinToString("") { byte ->
                    if (byte in 32..126) {
                        byte.toInt().toChar().toString()
                    } else {
                        "."
                    }
                }
                "ASCII: $asciiRepresentation\nHex: ${response.joinToString(" ") { "%02X".format(it) }}"
            } else {
                "Empty response from NFC tag"
            }
        } catch (e: Exception) {
            "Error parsing NFC response: ${e.message}"
        }
    }

    fun clearNfcData() {
        _nfcTagData.value = emptyMap()
        _rawResponse.value = ""
        _error.value = null
    }
}
