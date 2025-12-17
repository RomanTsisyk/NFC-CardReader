package io.github.romantsisyk.nfccardreader.presentation.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
import io.github.romantsisyk.nfccardreader.domain.model.NfcError
import io.github.romantsisyk.nfccardreader.domain.model.NfcResult
import io.github.romantsisyk.nfccardreader.domain.repository.NfcAvailability
import io.github.romantsisyk.nfccardreader.domain.repository.NfcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the NFC Reader screen.
 */
data class NfcUiState(
    val isLoading: Boolean = false,
    val nfcTagData: Map<String, String> = emptyMap(),
    val rawResponse: String = "Empty NFC Response",
    val additionalInfo: NFCData? = null,
    val error: NfcError? = null,
    val errorMessage: String? = null,
    val nfcAvailability: NfcAvailability? = null,
    val lastScanSaved: Boolean = false
)

/**
 * ViewModel for NFC Reader functionality.
 *
 * Manages UI state and coordinates between the UI layer and repository.
 *
 * @property repository Repository for NFC operations and data persistence
 */
@HiltViewModel
class NFCReaderViewModel @Inject constructor(
    private val repository: NfcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NfcUiState())
    val uiState: StateFlow<NfcUiState> = _uiState

    // Legacy state flows for backward compatibility
    val nfcTagData: StateFlow<Map<String, String>>
        get() = MutableStateFlow(_uiState.value.nfcTagData)

    val rawResponse: StateFlow<String>
        get() = MutableStateFlow(_uiState.value.rawResponse)

    val error: StateFlow<String?>
        get() = MutableStateFlow(_uiState.value.errorMessage)

    val additionalInfo: StateFlow<NFCData?>
        get() = MutableStateFlow(_uiState.value.additionalInfo)

    /**
     * Flow of scan history from the database.
     */
    val scanHistory = repository.getScanHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        checkNfcAvailability()
    }

    /**
     * Checks NFC availability on the device.
     */
    fun checkNfcAvailability() {
        when (val result = repository.checkNfcAvailability()) {
            is NfcResult.Success -> {
                _uiState.value = _uiState.value.copy(nfcAvailability = result.data)
            }
            is NfcResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    error = result.error,
                    errorMessage = result.message
                )
            }
            is NfcResult.Loading -> { /* Not expected here */ }
        }
    }

    /**
     * Processes an NFC intent and updates UI state.
     *
     * @param intent The NFC intent to process
     */
    fun processNfcIntent(intent: Intent) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, errorMessage = null)

            when (val result = repository.processNfcIntent(intent)) {
                is NfcResult.Success -> {
                    val data = result.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        nfcTagData = data.parsedTlvData,
                        rawResponse = data.rawResponse,
                        additionalInfo = data,
                        error = null,
                        errorMessage = null,
                        lastScanSaved = false
                    )
                }
                is NfcResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error,
                        errorMessage = result.message
                    )
                }
                is NfcResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    /**
     * Saves the current scan to history.
     */
    fun saveCurrentScan() {
        val currentData = _uiState.value.additionalInfo ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = repository.saveScanRecord(currentData)) {
                is NfcResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastScanSaved = true
                    )
                }
                is NfcResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error,
                        errorMessage = result.message
                    )
                }
                is NfcResult.Loading -> { /* Not expected here */ }
            }
        }
    }

    /**
     * Deletes a scan record from history.
     *
     * @param id The ID of the scan to delete
     */
    fun deleteScan(id: Long) {
        viewModelScope.launch {
            repository.deleteScan(id)
        }
    }

    /**
     * Clears all scan history.
     */
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    /**
     * Clears the current NFC data and resets UI state.
     */
    fun clearNfcData() {
        _uiState.value = NfcUiState(nfcAvailability = _uiState.value.nfcAvailability)
    }

    /**
     * Loads mock NFC data for testing purposes.
     */
    fun processMockNfcIntent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

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
                    "Application PAN" to "XXXX XXXX XXXX 1234",
                    "Track2 Equivalent Data" to "XXXX...XXXX",
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

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                nfcTagData = mockData.parsedTlvData,
                rawResponse = mockData.rawResponse,
                additionalInfo = mockData,
                error = null,
                errorMessage = null,
                lastScanSaved = false
            )
        }
    }
}