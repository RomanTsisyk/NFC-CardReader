package io.github.romantsisyk.nfccardreader.domain.repository

import android.content.Intent
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
import io.github.romantsisyk.nfccardreader.domain.model.NfcResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for NFC operations.
 *
 * This interface defines the contract for NFC data operations,
 * providing abstraction over the data source (NFC hardware, local database).
 */
interface NfcRepository {

    /**
     * Processes an NFC intent and returns card data.
     *
     * @param intent The NFC intent containing tag information
     * @return NfcResult containing either the parsed NFCData or an error
     */
    suspend fun processNfcIntent(intent: Intent): NfcResult<NFCData>

    /**
     * Saves a scan record to the local database.
     *
     * @param nfcData The NFC data to save
     * @return NfcResult indicating success or failure
     */
    suspend fun saveScanRecord(nfcData: NFCData): NfcResult<Long>

    /**
     * Retrieves all scan history from the local database.
     *
     * @return Flow emitting list of scan records
     */
    fun getScanHistory(): Flow<List<NFCData>>

    /**
     * Retrieves a specific scan record by ID.
     *
     * @param id The ID of the scan record
     * @return NfcResult containing the scan record or an error
     */
    suspend fun getScanById(id: Long): NfcResult<NFCData>

    /**
     * Deletes a scan record from the local database.
     *
     * @param id The ID of the scan record to delete
     * @return NfcResult indicating success or failure
     */
    suspend fun deleteScan(id: Long): NfcResult<Unit>

    /**
     * Clears all scan history from the local database.
     *
     * @return NfcResult indicating success or failure
     */
    suspend fun clearHistory(): NfcResult<Unit>

    /**
     * Checks if NFC is available and enabled on the device.
     *
     * @return NfcResult containing availability status or error
     */
    fun checkNfcAvailability(): NfcResult<NfcAvailability>
}

/**
 * Represents the NFC availability status on the device.
 */
data class NfcAvailability(
    val isAvailable: Boolean,
    val isEnabled: Boolean
)
