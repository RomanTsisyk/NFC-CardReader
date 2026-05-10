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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NfcUiState(
    val isLoading: Boolean = false,
    val nfcTagData: Map<String, String> = emptyMap(),
    val additionalInfo: NFCData? = null,
    val error: NfcError? = null,
    val errorMessage: String? = null,
    val nfcAvailability: NfcAvailability? = null,
    val lastScanSaved: Boolean = false
)

@HiltViewModel
class NFCReaderViewModel @Inject constructor(
    private val repository: NfcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NfcUiState())
    val uiState: StateFlow<NfcUiState> = _uiState

    val scanHistory = repository.getScanHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        checkNfcAvailability()
    }

    fun checkNfcAvailability() {
        when (val result = repository.checkNfcAvailability()) {
            is NfcResult.Success -> _uiState.update { it.copy(nfcAvailability = result.data) }
            is NfcResult.Error -> _uiState.update {
                it.copy(error = result.error, errorMessage = result.message)
            }
            is NfcResult.Loading -> Unit
        }
    }

    fun processNfcIntent(intent: Intent) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, errorMessage = null) }

            when (val result = repository.processNfcIntent(intent)) {
                is NfcResult.Success -> {
                    val data = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            nfcTagData = data.parsedTlvData,
                            additionalInfo = data,
                            error = null,
                            errorMessage = null,
                            lastScanSaved = false
                        )
                    }
                }
                is NfcResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.error, errorMessage = result.message)
                }
                is NfcResult.Loading -> _uiState.update { it.copy(isLoading = true) }
            }
        }
    }

    fun saveCurrentScan() {
        val currentData = _uiState.value.additionalInfo ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = repository.saveScanRecord(currentData)) {
                is NfcResult.Success -> _uiState.update {
                    it.copy(isLoading = false, lastScanSaved = true)
                }
                is NfcResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.error, errorMessage = result.message)
                }
                is NfcResult.Loading -> Unit
            }
        }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            when (val result = repository.deleteScan(id)) {
                is NfcResult.Success -> Unit
                is NfcResult.Error -> _uiState.update {
                    it.copy(error = result.error, errorMessage = result.message)
                }
                is NfcResult.Loading -> Unit
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            when (val result = repository.clearHistory()) {
                is NfcResult.Success -> Unit
                is NfcResult.Error -> _uiState.update {
                    it.copy(error = result.error, errorMessage = result.message)
                }
                is NfcResult.Loading -> Unit
            }
        }
    }

    fun clearNfcData() {
        _uiState.update { NfcUiState(nfcAvailability = it.nfcAvailability) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null, errorMessage = null) }
    }
}
