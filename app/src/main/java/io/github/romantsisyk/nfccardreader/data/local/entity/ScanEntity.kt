package io.github.romantsisyk.nfccardreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.romantsisyk.nfccardreader.domain.model.NFCData

@Entity(tableName = "scan_history")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val cardType: String?,
    val applicationLabel: String?,
    val maskedPan: String?,
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
    fun toNFCData(parsedTlvData: Map<String, String>): NFCData {
        return NFCData(
            dbId = id,
            rawResponse = "",
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
        fun fromNFCData(nfcData: NFCData, parsedTlvDataJson: String): ScanEntity {
            return ScanEntity(
                cardType = nfcData.cardType,
                applicationLabel = nfcData.applicationLabel,
                maskedPan = nfcData.parsedTlvData["APPLICATION_PAN"],
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
