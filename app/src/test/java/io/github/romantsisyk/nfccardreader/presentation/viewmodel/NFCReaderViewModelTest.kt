package io.github.romantsisyk.nfccardreader.presentation.viewmodel

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
import io.github.romantsisyk.nfccardreader.domain.model.NfcError
import io.github.romantsisyk.nfccardreader.domain.model.NfcResult
import io.github.romantsisyk.nfccardreader.domain.repository.NfcAvailability
import io.github.romantsisyk.nfccardreader.domain.repository.NfcRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class NFCReaderViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: NFCReaderViewModel
    private lateinit var fakeRepository: FakeNfcRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeNfcRepository()
        viewModel = NFCReaderViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty data and no error`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.nfcTagData.isEmpty())
        assertNull(state.additionalInfo)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `NFC availability is checked on init`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val availability = viewModel.uiState.value.nfcAvailability
        assertNotNull(availability)
        assertTrue(availability!!.isAvailable)
        assertTrue(availability.isEnabled)
    }

    @Test
    fun `processNfcIntent success updates state with card data`() = runTest {
        val fakeIntent = Intent()
        viewModel.processNfcIntent(fakeIntent)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNotNull(state.additionalInfo)
        assertEquals("Test Card", state.additionalInfo?.cardType)
        assertEquals(mapOf("Test" to "Data"), state.nfcTagData)
    }

    @Test
    fun `processNfcIntent error updates errorMessage`() = runTest {
        fakeRepository.shouldFailNextIntent = true
        viewModel.processNfcIntent(Intent())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals(NfcError.COMMUNICATION_ERROR, state.error)
    }

    @Test
    fun `clearNfcData resets state but preserves nfcAvailability`() = runTest {
        viewModel.processNfcIntent(Intent())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearNfcData()
        val state = viewModel.uiState.value
        assertTrue(state.nfcTagData.isEmpty())
        assertNull(state.additionalInfo)
        assertNull(state.errorMessage)
        assertNotNull(state.nfcAvailability)
    }

    @Test
    fun `saveCurrentScan with no data is no-op`() = runTest {
        viewModel.saveCurrentScan()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.lastScanSaved)
    }

    @Test
    fun `saveCurrentScan with data sets lastScanSaved to true`() = runTest {
        viewModel.processNfcIntent(Intent())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveCurrentScan()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lastScanSaved)
    }

    @Test
    fun `deleteScan error updates errorMessage`() = runTest {
        fakeRepository.shouldFailDelete = true
        viewModel.deleteScan(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearHistory error updates errorMessage`() = runTest {
        fakeRepository.shouldFailClear = true
        viewModel.clearHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `dismissError clears error state`() = runTest {
        fakeRepository.shouldFailNextIntent = true
        viewModel.processNfcIntent(Intent())
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.dismissError()
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.error)
    }
}

private class FakeNfcRepository : NfcRepository {

    var shouldFailNextIntent = false
    var shouldFailDelete = false
    var shouldFailClear = false

    private val _history = MutableStateFlow<List<NFCData>>(emptyList())

    override suspend fun processNfcIntent(intent: Intent): NfcResult<NFCData> {
        return if (shouldFailNextIntent) {
            shouldFailNextIntent = false
            NfcResult.Error(NfcError.COMMUNICATION_ERROR, "Simulated error")
        } else {
            NfcResult.Success(
                NFCData(rawResponse = "TEST", cardType = "Test Card", parsedTlvData = mapOf("Test" to "Data"))
            )
        }
    }

    override suspend fun saveScanRecord(nfcData: NFCData): NfcResult<Long> {
        _history.value = _history.value + nfcData
        return NfcResult.Success(_history.value.size.toLong())
    }

    override fun getScanHistory(): Flow<List<NFCData>> = _history

    override suspend fun getScanById(id: Long): NfcResult<NFCData> {
        val scan = _history.value.getOrNull(id.toInt() - 1)
        return if (scan != null) NfcResult.Success(scan)
        else NfcResult.Error(NfcError.INVALID_DATA, "Not found")
    }

    override suspend fun deleteScan(id: Long): NfcResult<Unit> {
        return if (shouldFailDelete) NfcResult.Error(NfcError.UNKNOWN_ERROR, "Delete failed")
        else NfcResult.Success(Unit)
    }

    override suspend fun clearHistory(): NfcResult<Unit> {
        return if (shouldFailClear) {
            NfcResult.Error(NfcError.UNKNOWN_ERROR, "Clear failed")
        } else {
            _history.value = emptyList()
            NfcResult.Success(Unit)
        }
    }

    override fun checkNfcAvailability(): NfcResult<NfcAvailability> {
        return NfcResult.Success(NfcAvailability(isAvailable = true, isEnabled = true))
    }
}
