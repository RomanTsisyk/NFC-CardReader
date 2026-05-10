package io.github.romantsisyk.nfccardreader.domain.usecase

import io.github.romantsisyk.nfccardreader.domain.EmvTag
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
import io.github.romantsisyk.nfccardreader.utils.NfcDataDecoder
import javax.inject.Inject

/**
 * Translates a parsed TLV map (output of ParseTLVUseCase) into a human-readable NFCData.
 * No TLV walking here — all tag extraction is done by ParseTLVUseCase.
 */
class InterpretNfcDataUseCase @Inject constructor() {

    fun execute(parsed: Map<String, String>): NFCData {
        return NFCData(
            cardType               = detectCardType(parsed),
            applicationLabel       = parsed[EmvTag.APPLICATION_LABEL.name]?.hexToUtf8(),
            transactionAmount      = parsed[EmvTag.TRANSACTION_AMOUNT.name]
                                         ?.chunked(2)?.let { NfcDataDecoder.decodeAmount(it) },
            currencyCode           = parsed[EmvTag.CURRENCY_CODE.name]
                                         ?.chunked(2)?.let { NfcDataDecoder.decodeCurrency(it) },
            transactionDate        = parsed[EmvTag.TRANSACTION_DATE.name]
                                         ?.chunked(2)?.let { NfcDataDecoder.decodeDate(it) },
            transactionStatus      = null, // SW1/SW2 is stripped before parsing — not a TLV field
            applicationIdentifier  = parsed[EmvTag.APPLICATION_IDENTIFIER.name]
                                         ?.chunked(2)?.let { NfcDataDecoder.decodeApplicationIdentifier(it) },
            applicationTemplate    = parsed[EmvTag.APPLICATION_TEMPLATE.name],
            dedicatedFileName      = parsed[EmvTag.DEDICATED_FILE_NAME.name],
            issuerCountryCode      = parsed[EmvTag.ISSUER_COUNTRY_CODE.name]
                                         ?.chunked(2)?.let { NfcDataDecoder.decodeCountryCode(it) },
            transactionCurrencyExponent = parsed[EmvTag.TRANSACTION_CURRENCY_EXPONENT.name],
            serviceCode            = parsed[EmvTag.SERVICE_CODE.name]
                                         ?.chunked(2)?.let { NfcDataDecoder.decodeServiceCode(it) },
            issuerUrl              = parsed[EmvTag.ISSUER_URL.name]?.hexToUtf8(),
            paymentAccountReference     = parsed[EmvTag.PAYMENT_ACCOUNT_REFERENCE.name],
            applicationCryptogram       = parsed[EmvTag.APPLICATION_CRYPTOGRAM.name],
            applicationTransactionCounter = parsed[EmvTag.APPLICATION_TRANSACTION_COUNTER.name],
            applicationInterchangeProfile = parsed[EmvTag.APPLICATION_INTERCHANGE_PROFILE.name],
            terminalVerificationResults = parsed[EmvTag.TERMINAL_VERIFICATION_RESULTS.name],
            transactionType        = parsed[EmvTag.TRANSACTION_TYPE.name]
                                         ?.take(2)?.let { NfcDataDecoder.decodeTransactionType(it) },
            issuerApplicationData  = parsed[EmvTag.ISSUER_APPLICATION_DATA.name],
            terminalCountryCode    = parsed[EmvTag.TERMINAL_COUNTRY_CODE.name]
                                         ?.chunked(2)?.let { NfcDataDecoder.decodeCountryCode(it) },
            interfaceDeviceSerialNumber = parsed[EmvTag.INTERFACE_DEVICE_SERIAL_NUMBER.name],
            unpredictableNumber    = parsed[EmvTag.UNPREDICTABLE_NUMBER.name],
            cardholderVerificationMethodResults = parsed[EmvTag.CARDHOLDER_VERIFICATION_METHOD_RESULTS.name]
                                         ?.chunked(2)?.let { NfcDataDecoder.decodeCardholderVerificationMethodResult(it) },
            issuerScriptResults    = parsed[EmvTag.ISSUER_SCRIPT_RESULTS.name],
            applicationCurrencyCode     = parsed[EmvTag.APPLICATION_CURRENCY_CODE.name],
            transactionCategoryCode     = parsed[EmvTag.TRANSACTION_CATEGORY_CODE.name],
            formFactorIndicator    = parsed[EmvTag.FORM_FACTOR_INDICATOR.name]
                                         ?.chunked(2)?.let { NfcDataDecoder.decodeFormFactorIndicator(it) },
            parsedTlvData          = parsed
        )
    }

    private fun detectCardType(parsed: Map<String, String>): String? {
        val aidHex = parsed[EmvTag.APPLICATION_IDENTIFIER_ADDITIONAL.name]
            ?: parsed[EmvTag.APPLICATION_IDENTIFIER.name]
            ?: return null
        return NfcDataDecoder.decodeApplicationIdentifier(aidHex.chunked(2))
    }

    private fun String.hexToUtf8(): String? = runCatching {
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            .toString(Charsets.UTF_8).trim().ifEmpty { null }
    }.getOrNull()
}
