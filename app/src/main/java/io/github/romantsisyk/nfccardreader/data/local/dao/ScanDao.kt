package io.github.romantsisyk.nfccardreader.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.romantsisyk.nfccardreader.data.local.entity.ScanEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for scan history operations.
 *
 * Provides methods for CRUD operations on scan records.
 */
@Dao
interface ScanDao {

    /**
     * Inserts a new scan record into the database.
     *
     * @param scan The scan entity to insert
     * @return The row ID of the inserted record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: ScanEntity): Long

    /**
     * Retrieves all scan records ordered by timestamp (newest first).
     *
     * @return Flow emitting list of scan entities
     */
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    /**
     * Retrieves a specific scan record by ID.
     *
     * @param id The ID of the scan record
     * @return The scan entity or null if not found
     */
    @Query("SELECT * FROM scan_history WHERE id = :id")
    suspend fun getScanById(id: Long): ScanEntity?

    /**
     * Deletes a scan record by ID.
     *
     * @param id The ID of the scan record to delete
     */
    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Deletes all scan records from the database.
     */
    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()

    /**
     * Gets the total count of scan records.
     *
     * @return The number of scan records
     */
    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun getCount(): Int
}
