package io.github.romantsisyk.nfccardreader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.romantsisyk.nfccardreader.data.local.dao.ScanDao
import io.github.romantsisyk.nfccardreader.data.local.entity.ScanEntity

@Database(
    entities = [ScanEntity::class],
    version = 2,
    exportSchema = true
)
abstract class NfcDatabase : RoomDatabase() {

    abstract fun scanDao(): ScanDao

    companion object {
        const val DATABASE_NAME = "nfc_card_reader_db"

        // v1 → v2: drop rawResponse and expirationDate columns (contain unmasked card data)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE scan_history_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        cardType TEXT,
                        applicationLabel TEXT,
                        maskedPan TEXT,
                        transactionAmount TEXT,
                        currencyCode TEXT,
                        transactionDate TEXT,
                        transactionStatus TEXT,
                        applicationIdentifier TEXT,
                        issuerCountryCode TEXT,
                        serviceCode TEXT,
                        formFactorIndicator TEXT,
                        parsedTlvDataJson TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO scan_history_new (
                        id, timestamp, cardType, applicationLabel, maskedPan,
                        transactionAmount, currencyCode, transactionDate, transactionStatus,
                        applicationIdentifier, issuerCountryCode, serviceCode,
                        formFactorIndicator, parsedTlvDataJson
                    )
                    SELECT
                        id, timestamp, cardType, applicationLabel, maskedPan,
                        transactionAmount, currencyCode, transactionDate, transactionStatus,
                        applicationIdentifier, issuerCountryCode, serviceCode,
                        formFactorIndicator, parsedTlvDataJson
                    FROM scan_history
                """.trimIndent())
                db.execSQL("DROP TABLE scan_history")
                db.execSQL("ALTER TABLE scan_history_new RENAME TO scan_history")
            }
        }
    }
}
