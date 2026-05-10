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

@Singleton
class NfcRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processNfcIntentUseCase: ProcessNfcIntentUseCase,
    private val scanDao: ScanDao
) : NfcRepository {

    override suspend fun processNfcIntent(intent: Intent): NfcResult<NFCData> {
        return withContext(Dispatchers.IO) {
            try {
                NfcResult.Success(processNfcIntentUseCase.execute(intent))
            } catch (e: IllegalArgumentException) {
                // PCI-DSS: do not propagate raw exception — its message/toString may contain
                // unmasked TLV bytes, PAN, or track data parsed from the card.
                NfcResult.Error(NfcError.TAG_NOT_FOUND, "No NFC tag found (${e.javaClass.simpleName})")
            } catch (e: UnsupportedOperationException) {
                NfcResult.Error(NfcError.UNSUPPORTED_TAG, "Unsupported NFC tag type (${e.javaClass.simpleName})")
            } catch (e: IllegalStateException) {
                NfcResult.Error(NfcError.INITIALIZATION_ERROR, "Initialization error (${e.javaClass.simpleName})")
            } catch (e: Exception) {
                NfcResult.Error(NfcError.COMMUNICATION_ERROR, "Communication error with NFC tag (${e.javaClass.simpleName})")
            }
        }
    }

    override suspend fun saveScanRecord(nfcData: NFCData): NfcResult<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val json = serializeTlvMap(nfcData.parsedTlvData)
                val entity = ScanEntity.fromNFCData(nfcData, json)
                NfcResult.Success(scanDao.insert(entity))
            } catch (e: Exception) {
                // Scrub: a Room/SQLite exception message may include the offending row values.
                NfcResult.Error(NfcError.UNKNOWN_ERROR, "Failed to save scan record (${e.javaClass.simpleName})")
            }
        }
    }

    override fun getScanHistory(): Flow<List<NFCData>> {
        return scanDao.getAllScans().map { entities ->
            entities.map { entity ->
                entity.toNFCData(deserializeTlvMap(entity.parsedTlvDataJson))
            }
        }
    }

    override suspend fun getScanById(id: Long): NfcResult<NFCData> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = scanDao.getScanById(id)
                if (entity != null) {
                    NfcResult.Success(entity.toNFCData(deserializeTlvMap(entity.parsedTlvDataJson)))
                } else {
                    NfcResult.Error(NfcError.INVALID_DATA, "Scan record not found")
                }
            } catch (e: Exception) {
                NfcResult.Error(NfcError.UNKNOWN_ERROR, "Failed to retrieve scan record (${e.javaClass.simpleName})")
            }
        }
    }

    override suspend fun deleteScan(id: Long): NfcResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                scanDao.deleteById(id)
                NfcResult.Success(Unit)
            } catch (e: Exception) {
                NfcResult.Error(NfcError.UNKNOWN_ERROR, "Failed to delete scan record (${e.javaClass.simpleName})")
            }
        }
    }

    override suspend fun clearHistory(): NfcResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                scanDao.deleteAll()
                NfcResult.Success(Unit)
            } catch (e: Exception) {
                NfcResult.Error(NfcError.UNKNOWN_ERROR, "Failed to clear history (${e.javaClass.simpleName})")
            }
        }
    }

    override fun checkNfcAvailability(): NfcResult<NfcAvailability> {
        return try {
            val adapter = context.getSystemService(NfcAdapter::class.java)
            NfcResult.Success(
                NfcAvailability(
                    isAvailable = adapter != null,
                    isEnabled = adapter?.isEnabled == true
                )
            )
        } catch (e: Exception) {
            NfcResult.Error(NfcError.UNKNOWN_ERROR, "Failed to check NFC availability (${e.javaClass.simpleName})")
        }
    }

    private fun serializeTlvMap(map: Map<String, String>): String {
        val obj = JSONObject()
        map.filterKeys { it !in SENSITIVE_TAGS }.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    companion object {
        // Never persist these tags — they contain sensitive card data per PCI-DSS.
        // Keys must match EmvTag.name values (or "Tag XXXX" fallback keys from ParseTLVUseCase).
        private val SENSITIVE_TAGS = setOf(
            // Cardholder identity
            "CARDHOLDER_NAME",
            // Card authentication data
            "EXPIRATION_DATE",
            "TRACK2_EQUIVALENT_DATA",
            // PAN sequence (tag 5F34 — parser emits "Tag 5F34" for unknown tags)
            "Tag 5F34",
            // Track 1 discretionary data (tag 9F1F — not in enum, stored as raw tag key)
            "Tag 9F1F",
            // Issuer-specific data that may contain raw track data
            "Tag 56",
            // DF Name (tag 84) — application identifier that can fingerprint cardholder app selection
            "Tag 84",
            // CVM Results (tag 9F34) — cardholder verification method outcome,
            // may leak whether PIN/signature was used and the result byte
            "Tag 9F34",
            "CVM_RESULTS"
        )
    }

    private fun deserializeTlvMap(json: String): Map<String, String> {
        return try {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { obj.getString(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
