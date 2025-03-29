package io.github.romantsisyk.nfccardreader.presentation.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
import io.github.romantsisyk.nfccardreader.domain.usecase.ProcessNfcIntentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NFCReaderViewModel @Inject constructor(
    private val processNfcIntentUseCase: ProcessNfcIntentUseCase?
) : ViewModel() {

    private val _nfcTagData = MutableStateFlow<Map<String, String>>(emptyMap())
    val nfcTagData: StateFlow<Map<String, String>> get() = _nfcTagData

    private val _rawResponse = MutableStateFlow("Empty NFC Response")
    val rawResponse: StateFlow<String> get() = _rawResponse

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    private val _additionalInfo = MutableStateFlow<NFCData?>(null)
    val additionalInfo: StateFlow<NFCData?> get() = _additionalInfo

    fun processNfcIntent(intent: Intent) {
        viewModelScope.launch {
            try {
                if (processNfcIntentUseCase == null) {
                    _error.value = "ProcessNfcIntentUseCase is not initialized (testing only)"
                    return@launch
                }

                val data = processNfcIntentUseCase.execute(intent)
                _nfcTagData.value = data.parsedTlvData
                _rawResponse.value = data.rawResponse
                _additionalInfo.value = data
                _error.value = null // Clear errors
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun setError(message: String) {
        _error.value = message
    }

    fun clearNfcData() {
        _nfcTagData.value = emptyMap()
        _rawResponse.value = "Empty NFC Response"
        _error.value = null
        _additionalInfo.value = null
    }

    fun processMockNfcIntent() {
        viewModelScope.launch {
            val mockData = NFCData(
                rawResponse = "6F 37 84 0E 325041592E5359532E4444463031 A5 25 88 01 02 5F 2D 02 656E " +
                        "9F 11 01 01 50 0A 4D617374657243617264 87 01 01 9F 38 06 9F1A029505 " +
                        "5F 20 0F 4A4F484E20512E2050554254494320 5A 0B 5412345678901234 5F 24 03 250228 " +
                        "5F 30 02 0201 9F 0D 05 B0600000 9F 0E 05 0010000000 9F 0F 05 B0604000 " +
                        "9F 12 0A 4D6173746572436172 57 13 5412345678901234D250228753622340 " +
                        "5F 28 02 0840 9F 1C 08 30303030303132 9F 1E 08 3132333435363738 " +
                        "9F 36 02 0003 82 02 1980 95 05 0080008000 9C 01 00 " +
                        "9F 02 06 000000002500 5F 2A 02 0840 9A 03 230420 9F 21 03 103542 " +
                        "9F 26 08 A1B2C3D4E5F6789A 9F 27 01 80 9F 34 03 1E0305 9F 37 04 FE9A8B21 " +
                        "9F 10 12 0110A00003220000000000000000000F 9F 6E 04 04210103 90 00",
                parsedTlvData = mapOf(
                    "Cardholder Name" to "JOHN Q. PUBLIC",
                    "Application PAN" to "5412 3456 7890 1234",
                    "Track2 Equivalent Data" to "5412345678901234D250228753622340",
                    "Expiration Date" to "02/25",
                    "Application Preferred Name" to "MasterCard",
                    "Service Code" to "201",
                    "Issuer Country Code" to "USA",
                    "Dedicated File Name" to "2PAY.SYS.DDF01",
                    "Application Cryptogram" to "A1B2C3D4E5F6789A",
                    "Terminal Verification Results" to "0080008000",
                    "Application Transaction Counter" to "0003",
                    "Interface Device Serial Number" to "12345678",
                    "Terminal ID" to "00000012",
                    "Application Interchange Profile" to "1980",
                    "Issuer Application Data" to "0110A00003220000000000000000000F",
                    "Form Factor Indicator" to "04210103",
                    "Language Preference" to "en",
                    "Unpredictable Number" to "FE9A8B21"
                ),
                cardType = "MasterCard Credit",
                applicationLabel = "MasterCard",
                transactionAmount = "25.00",
                currencyCode = "USD",
                transactionDate = "20.04.2023",
                transactionStatus = "Successful",
                applicationIdentifier = "MasterCard",
                dedicatedFileName = "2PAY.SYS.DDF01",
                issuerCountryCode = "USA",
                serviceCode = "International interchange, By issuer, No restrictions",
                formFactorIndicator = "Physical card with contact chip and contactless",
                applicationTemplate = "A5 25 88 01 02 5F 2D 02 656E 9F 11 01 01",
                unpredictableNumber = "FE9A8B21",
                cardholderVerificationMethodResults = "Online PIN verified",
                applicationCryptogram = "A1B2C3D4E5F6789A",
                applicationTransactionCounter = "0003",
                applicationInterchangeProfile = "1980",
                terminalVerificationResults = "0080008000",
                transactionType = "Purchase",
                issuerApplicationData = "0110A00003220000000000000000000F",
                terminalCountryCode = "USA",
                interfaceDeviceSerialNumber = "12345678"
            )
            _nfcTagData.value = mockData.parsedTlvData
            _rawResponse.value = mockData.rawResponse
            _additionalInfo.value = mockData
            _error.value = null // Clear errors
        }
    }
}