package io.github.romantsisyk.nfccardreader.data.repository

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.romantsisyk.nfccardreader.data.local.dao.ScanDao
import io.github.romantsisyk.nfccardreader.data.local.entity.ScanEntity
import io.github.romantsisyk.nfccardreader.domain.model.NFCData
import io.github.romantsisyk.nfccardreader.domain.model.NfcError
import io.github.romantsisyk.nfccardreader.domain.model.NfcResult
import io.github.romantsisyk.nfccardreader.domain.repository.NfcAvailability
import io.github.romantsisyk.nfccardreader.domain.repository.NfcRepository
import io.github.romantsisyk.nfccardreader.domain.usecase.ProcessNfcIntentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [NfcRepository] that handles NFC operations
 * and local database storage.
 *
 * @property context Application context for NFC adapter access
 * @property processNfcIntentUseCase Use case for processing NFC intents
 * @property scanDao DAO for scan history database operations
 */
@Singleton
class NfcRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processNfcIntentUseCase: ProcessNfcIntentUseCase,
    private val scanDao: ScanDao
) : NfcRepository {

    override suspend fun processNfcIntent(intent: Intent): NfcResult<NFCData> {
        return withContext(Dispatchers.IO) {
            try {
                val nfcData = processNfcIntentUseCase.execute(intent)
                NfcResult.Success(nfcData)
            } catch (e: IllegalArgumentException) {
                NfcResult.Error(
                    error = NfcError.TAG_NOT_FOUND,
                    message = e.message ?: "No NFC tag found",
                    exception = e
                )
            } catch (e: UnsupportedOperationException) {
                NfcResult.Error(
                    error = NfcError.UNSUPPORTED_TAG,
                    message = e.message ?: "Unsupported NFC tag type",
                    exception = e
                )
            } catch (e: IllegalStateException) {
                NfcResult.Error(
                    error = NfcError.INITIALIZATION_ERROR,
                    message = e.message ?: "Initialization error",
                    exception = e
                )
            } catch (e: Exception) {
                NfcResult.Error(
                    error = NfcError.COMMUNICATION_ERROR,
                    message = e.message ?: "Communication error with NFC tag",
                    exception = e
                )
            }
        }
    }

    override suspend fun saveScanRecord(nfcData: NFCData): NfcResult<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject()
                nfcData.parsedTlvData.forEach { (key, value) ->
                    jsonObject.put(key, value)
                }
                val entity = ScanEntity.fromNFCData(nfcData, jsonObject.toString())
                val id = scanDao.insert(entity)
                NfcResult.Success(id)
            } catch (e: Exception) {
                NfcResult.Error(
                    error = NfcError.UNKNOWN_ERROR,
                    message = "Failed to save scan record: ${e.message}",
                    exception = e
                )
            }
        }
    }

    override fun getScanHistory(): Flow<List<NFCData>> {
        return scanDao.getAllScans().map { entities ->
            entities.map { entity ->
                val parsedTlvData = try {
                    val jsonObject = JSONObject(entity.parsedTlvDataJson)
                    val map = mutableMapOf<String, String>()
                    jsonObject.keys().forEach { key ->
                        map[key] = jsonObject.getString(key)
                    }
                    map
                } catch (e: Exception) {
                    emptyMap()
                }
                entity.toNFCData(parsedTlvData)
            }
        }
    }

    override suspend fun getScanById(id: Long): NfcResult<NFCData> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = scanDao.getScanById(id)
                if (entity != null) {
                    val parsedTlvData = try {
                        val jsonObject = JSONObject(entity.parsedTlvDataJson)
                        val map = mutableMapOf<String, String>()
                        jsonObject.keys().forEach { key ->
                            map[key] = jsonObject.getString(key)
                        }
                        map
                    } catch (e: Exception) {
                        emptyMap()
                    }
                    NfcResult.Success(entity.toNFCData(parsedTlvData))
                } else {
                    NfcResult.Error(
                        error = NfcError.INVALID_DATA,
                        message = "Scan record not found"
                    )
                }
            } catch (e: Exception) {
                NfcResult.Error(
                    error = NfcError.UNKNOWN_ERROR,
                    message = "Failed to retrieve scan record: ${e.message}",
                    exception = e
                )
            }
        }
    }

    override suspend fun deleteScan(id: Long): NfcResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                scanDao.deleteById(id)
                NfcResult.Success(Unit)
            } catch (e: Exception) {
                NfcResult.Error(
                    error = NfcError.UNKNOWN_ERROR,
                    message = "Failed to delete scan record: ${e.message}",
                    exception = e
                )
            }
        }
    }

    override suspend fun clearHistory(): NfcResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                scanDao.deleteAll()
                NfcResult.Success(Unit)
            } catch (e: Exception) {
                NfcResult.Error(
                    error = NfcError.UNKNOWN_ERROR,
                    message = "Failed to clear history: ${e.message}",
                    exception = e
                )
            }
        }
    }

    override fun checkNfcAvailability(): NfcResult<NfcAvailability> {
        return try {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
            if (nfcAdapter == null) {
                NfcResult.Success(NfcAvailability(isAvailable = false, isEnabled = false))
            } else {
                NfcResult.Success(
                    NfcAvailability(
                        isAvailable = true,
                        isEnabled = nfcAdapter.isEnabled
                    )
                )
            }
        } catch (e: Exception) {
            NfcResult.Error(
                error = NfcError.UNKNOWN_ERROR,
                message = "Failed to check NFC availability: ${e.message}",
                exception = e
            )
        }
    }
}
