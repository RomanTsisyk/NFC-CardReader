package io.github.romantsisyk.nfccardreader.presentation.viewmodel

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
import io.github.romantsisyk.nfccardreader.domain.model.NfcResult
import io.github.romantsisyk.nfccardreader.domain.repository.NfcAvailability
import io.github.romantsisyk.nfccardreader.domain.repository.NfcRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for NFCReaderViewModel.
 * Tests state management and repository interactions.
 */
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
    fun `test initial state`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        // Initial state should have empty data
        val uiState = viewModel.uiState.value
        assertTrue(uiState.nfcTagData.isEmpty())
        assertEquals("Empty NFC Response", uiState.rawResponse)
        assertNull(uiState.additionalInfo)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `test clearNfcData`() = runTest {
        // First load some mock data
        viewModel.processMockNfcIntent()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then clear it
        viewModel.clearNfcData()

        // Check if data was cleared
        val uiState = viewModel.uiState.value
        assertTrue(uiState.nfcTagData.isEmpty())
        assertEquals("Empty NFC Response", uiState.rawResponse)
        assertNull(uiState.additionalInfo)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `test processMockNfcIntent`() = runTest {
        // When
        viewModel.processMockNfcIntent()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - verify the mock data contains expected fields
        val uiState = viewModel.uiState.value
        assertFalse(uiState.nfcTagData.isEmpty())
        assertNotEquals("Empty NFC Response", uiState.rawResponse)

        val mockData = uiState.additionalInfo
        assertNotNull(mockData)
        assertEquals("MasterCard Credit", mockData?.cardType)
        assertEquals("MasterCard", mockData?.applicationLabel)
        assertEquals("25.00", mockData?.transactionAmount)
        assertEquals("USD", mockData?.currencyCode)
    }

    @Test
    fun `test nfc availability check`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNotNull(uiState.nfcAvailability)
        assertTrue(uiState.nfcAvailability?.isAvailable == true)
        assertTrue(uiState.nfcAvailability?.isEnabled == true)
    }
}

/**
 * Fake repository implementation for testing.
 */
private class FakeNfcRepository : NfcRepository {

    private val scanHistory = mutableListOf<NFCData>()

    override suspend fun processNfcIntent(intent: Intent): NfcResult<NFCData> {
        return NfcResult.Success(
            NFCData(
                rawResponse = "TEST",
                cardType = "Test Card",
                parsedTlvData = mapOf("Test" to "Data")
            )
        )
    }

    override suspend fun saveScanRecord(nfcData: NFCData): NfcResult<Long> {
        scanHistory.add(nfcData)
        return NfcResult.Success(scanHistory.size.toLong())
    }

    override fun getScanHistory(): Flow<List<NFCData>> {
        return flowOf(scanHistory.toList())
    }

    override suspend fun getScanById(id: Long): NfcResult<NFCData> {
        val scan = scanHistory.getOrNull(id.toInt() - 1)
        return if (scan != null) {
            NfcResult.Success(scan)
        } else {
            NfcResult.Error(
                io.github.romantsisyk.nfccardreader.domain.model.NfcError.INVALID_DATA,
                "Not found"
            )
        }
    }

    override suspend fun deleteScan(id: Long): NfcResult<Unit> {
        return NfcResult.Success(Unit)
    }

    override suspend fun clearHistory(): NfcResult<Unit> {
        scanHistory.clear()
        return NfcResult.Success(Unit)
    }

    override fun checkNfcAvailability(): NfcResult<NfcAvailability> {
        return NfcResult.Success(NfcAvailability(isAvailable = true, isEnabled = true))
    }
}