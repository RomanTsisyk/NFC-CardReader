package io.github.romantsisyk.nfccardreader.domain.usecase

import android.util.Log
import io.github.romantsisyk.nfccardreader.domain.EmvTag
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
import io.github.romantsisyk.nfccardreader.utils.NfcDataDecoder

/**
 * Use case for interpreting raw NFC response data into structured NFCData.
 *
 * This class transforms raw byte arrays from NFC card communications into
 * human-readable, structured data. It handles EMV tag detection and delegates
 * specific value interpretation to [NfcDataDecoder].
 *
 * @see NFCData for the output data structure
 * @see NfcDataDecoder for value interpretation utilities
 */
class InterpretNfcDataUseCase {

    private val TAG = "InterpretNfcDataUseCase"

    /**
     * Executes interpretation of raw NFC response bytes.
     *
     * Parses the response byte array, identifies EMV tags, and constructs
     * a comprehensive [NFCData] object containing all interpreted fields.
     *
     * @param response Raw byte array from NFC card communication
     * @return NFCData object with all interpreted card information
     */
    fun execute(response: ByteArray): NFCData {
        val hexBytes = response.map { "%02X".format(it) }
        val interpretedData = mutableMapOf<EmvTag, String>()
        
        Log.d(TAG, "Starting NFC data interpretation. Response size: ${response.size}")

        var i = 0
        while (i < hexBytes.size) {
            val tag = if (i + 1 < hexBytes.size && hexBytes[i] == "5F" || hexBytes[i] == "9F") {
                "${hexBytes[i]}${hexBytes[i+1]}"
            } else {
                hexBytes[i]
            }
            
            val emvTag = EmvTag.fromTag(tag)
            
            Log.d(TAG, "Processing tag: $tag (${emvTag.name}) at index $i")
            
            if (tag.length > 2) {
                i++
            }
            
            if (emvTag != EmvTag.UNKNOWN && i + 1 < hexBytes.size) {
                val length = try {
                    hexBytes[i + 1].toInt(16)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing length for tag $tag", e)
                    1 // За замовчуванням припускаємо довжину 1
                }
                
                if (i + 1 + length < hexBytes.size) {
                    val valueBytes = hexBytes.subList(i + 2, i + 2 + length)
                    val interpretedValue = interpretTagValue(emvTag, valueBytes)
                    interpretedData[emvTag] = interpretedValue
                    Log.d(TAG, "Parsed ${emvTag.name}: $interpretedValue")
                    
                    i += 1 + length
                } else {
                    Log.w(TAG, "Not enough data remaining for tag $tag. Breaking.")
                    break
                }
            } else {
                i++
            }
        }
        
        Log.d(TAG, "Completed NFC data interpretation. Interpreted ${interpretedData.size} tags.")
        
        return NFCData(
            rawResponse = hexBytes.joinToString(" "),
            cardType = interpretedData[EmvTag.CARD_TYPE],
            applicationLabel = interpretedData[EmvTag.APPLICATION_LABEL],
            transactionAmount = interpretedData[EmvTag.TRANSACTION_AMOUNT],
            currencyCode = interpretedData[EmvTag.CURRENCY_CODE],
            transactionDate = interpretedData[EmvTag.TRANSACTION_DATE],
            transactionStatus = interpretedData[EmvTag.TRANSACTION_STATUS],
            applicationIdentifier = interpretedData[EmvTag.APPLICATION_IDENTIFIER],
            applicationTemplate = interpretedData[EmvTag.APPLICATION_TEMPLATE],
            dedicatedFileName = interpretedData[EmvTag.DEDICATED_FILE_NAME],
            issuerCountryCode = interpretedData[EmvTag.ISSUER_COUNTRY_CODE],
            transactionCurrencyExponent = interpretedData[EmvTag.TRANSACTION_CURRENCY_EXPONENT],
            serviceCode = interpretedData[EmvTag.SERVICE_CODE],
            issuerUrl = interpretedData[EmvTag.ISSUER_URL],
            paymentAccountReference = interpretedData[EmvTag.PAYMENT_ACCOUNT_REFERENCE],
            applicationCryptogram = interpretedData[EmvTag.APPLICATION_CRYPTOGRAM],
            applicationTransactionCounter = interpretedData[EmvTag.APPLICATION_TRANSACTION_COUNTER],
            applicationInterchangeProfile = interpretedData[EmvTag.APPLICATION_INTERCHANGE_PROFILE],
            terminalVerificationResults = interpretedData[EmvTag.TERMINAL_VERIFICATION_RESULTS],
            transactionType = interpretedData[EmvTag.TRANSACTION_TYPE],
            issuerApplicationData = interpretedData[EmvTag.ISSUER_APPLICATION_DATA],
            terminalCountryCode = interpretedData[EmvTag.TERMINAL_COUNTRY_CODE],
            interfaceDeviceSerialNumber = interpretedData[EmvTag.INTERFACE_DEVICE_SERIAL_NUMBER],
            unpredictableNumber = interpretedData[EmvTag.UNPREDICTABLE_NUMBER],
            cardholderVerificationMethodResults = interpretedData[EmvTag.CARDHOLDER_VERIFICATION_METHOD_RESULTS],
            issuerScriptResults = interpretedData[EmvTag.ISSUER_SCRIPT_RESULTS],
            applicationCurrencyCode = interpretedData[EmvTag.APPLICATION_CURRENCY_CODE],
            transactionCategoryCode = interpretedData[EmvTag.TRANSACTION_CATEGORY_CODE],
            formFactorIndicator = interpretedData[EmvTag.FORM_FACTOR_INDICATOR]
        )
    }
    
    private fun interpretTagValue(emvTag: EmvTag, valueBytes: List<String>): String {
        return when (emvTag) {
            EmvTag.CARD_TYPE -> {
                if (valueBytes.any { it.contains("A0") }) {
                    "EMV Payment Card"
                } else {
                    "Unknown Card Type"
                }
            }
            EmvTag.APPLICATION_LABEL -> {
                try {
                    val bytes = valueBytes.map { it.toInt(16).toByte() }.toByteArray()
                    String(bytes, Charsets.UTF_8)
                } catch (e: Exception) {
                    "Parsing Error"
                }
            }
            EmvTag.TRANSACTION_AMOUNT -> NfcDataDecoder.decodeAmount(valueBytes)
            EmvTag.CURRENCY_CODE -> NfcDataDecoder.decodeCurrency(valueBytes)
            EmvTag.TRANSACTION_DATE -> NfcDataDecoder.decodeDate(valueBytes)
            EmvTag.TRANSACTION_STATUS -> {
                if (valueBytes.firstOrNull() == "00") "Successful" else "Error"
            }
            EmvTag.APPLICATION_IDENTIFIER -> NfcDataDecoder.decodeApplicationIdentifier(valueBytes)
            EmvTag.SERVICE_CODE -> {
                if (valueBytes.size >= 3) {
                    NfcDataDecoder.decodeServiceCode(valueBytes.subList(0, 3))
                } else {
                    "Incomplete Service Code"
                }
            }
            EmvTag.TRANSACTION_TYPE -> {
                if (valueBytes.isNotEmpty()) {
                    NfcDataDecoder.decodeTransactionType(valueBytes.first())
                } else {
                    "Unknown Transaction Type"
                }
            }
            EmvTag.ISSUER_COUNTRY_CODE -> NfcDataDecoder.decodeCountryCode(valueBytes)
            EmvTag.TERMINAL_COUNTRY_CODE -> NfcDataDecoder.decodeCountryCode(valueBytes)
            EmvTag.CARDHOLDER_VERIFICATION_METHOD_RESULTS -> NfcDataDecoder.decodeCardholderVerificationMethodResult(valueBytes)
            EmvTag.FORM_FACTOR_INDICATOR -> NfcDataDecoder.decodeFormFactorIndicator(valueBytes)
            else -> {
                valueBytes.joinToString("")
            }
        }
    }
}
