package io.github.romantsisyk.nfccardreader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.romantsisyk.nfccardreader.data.local.dao.ScanDao
import io.github.romantsisyk.nfccardreader.data.local.entity.ScanEntity

/**
 * Room database for NFC Card Reader application.
 *
 * Contains the scan history table for storing NFC scan records.
 */
@Database(
    entities = [ScanEntity::class],
    version = 1,
    exportSchema = true
)
abstract class NfcDatabase : RoomDatabase() {

    /**
     * Provides access to scan history DAO.
     */
    abstract fun scanDao(): ScanDao

    companion object {
        const val DATABASE_NAME = "nfc_card_reader_db"
    }
}
