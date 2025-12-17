package io.github.romantsisyk.nfccardreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.romantsisyk.nfccardreader.domain.model.NFCData

/**
 * Room entity representing a saved NFC scan record.
 *
 * This entity stores the results of NFC card scans for history tracking.
 */
@Entity(tableName = "scan_history")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val rawResponse: String,
    val cardType: String?,
    val applicationLabel: String?,
    val maskedPan: String?,
    val expirationDate: String?,
    val transactionAmount: String?,
    val currencyCode: String?,
    val transactionDate: String?,
    val transactionStatus: String?,
    val applicationIdentifier: String?,
    val issuerCountryCode: String?,
    val serviceCode: String?,
    val formFactorIndicator: String?,
    val parsedTlvDataJson: String
) {
    /**
     * Converts this entity to domain model.
     *
     * @param parsedTlvData The deserialized TLV data map
     * @return NFCData domain model
     */
    fun toNFCData(parsedTlvData: Map<String, String>): NFCData {
        return NFCData(
            rawResponse = rawResponse,
            cardType = cardType,
            applicationLabel = applicationLabel,
            transactionAmount = transactionAmount,
            currencyCode = currencyCode,
            transactionDate = transactionDate,
            transactionStatus = transactionStatus,
            applicationIdentifier = applicationIdentifier,
            issuerCountryCode = issuerCountryCode,
            serviceCode = serviceCode,
            formFactorIndicator = formFactorIndicator,
            parsedTlvData = parsedTlvData
        )
    }

    companion object {
        /**
         * Creates an entity from domain model.
         *
         * @param nfcData The domain model
         * @param parsedTlvDataJson JSON string of parsed TLV data
         * @return ScanEntity for database storage
         */
        fun fromNFCData(nfcData: NFCData, parsedTlvDataJson: String): ScanEntity {
            return ScanEntity(
                rawResponse = nfcData.rawResponse,
                cardType = nfcData.cardType,
                applicationLabel = nfcData.applicationLabel,
                maskedPan = nfcData.parsedTlvData["Application PAN"],
                expirationDate = nfcData.parsedTlvData["Expiration Date"],
                transactionAmount = nfcData.transactionAmount,
                currencyCode = nfcData.currencyCode,
                transactionDate = nfcData.transactionDate,
                transactionStatus = nfcData.transactionStatus,
                applicationIdentifier = nfcData.applicationIdentifier,
                issuerCountryCode = nfcData.issuerCountryCode,
                serviceCode = nfcData.serviceCode,
                formFactorIndicator = nfcData.formFactorIndicator,
                parsedTlvDataJson = parsedTlvDataJson
            )
        }
    }
}
