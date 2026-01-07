package com.dagimg.glide.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for clipboard items.
 * Provides reactive queries using Flow for UI updates.
 */
@Dao
interface ClipboardDao {
    /**
     * Get all clipboard items ordered by: pinned first, then by timestamp descending
     */
    @Query(
        """
        SELECT * FROM clipboard_items 
        ORDER BY isPinned DESC, timestamp DESC
    """,
    )
    fun getAllOrdered(): Flow<List<ClipboardEntity>>

    /**
     * Get a single item by ID
     */
    @Query("SELECT * FROM clipboard_items WHERE id = :id")
    suspend fun getById(id: String): ClipboardEntity?

    /**
     * Check if text content already exists (for duplicate detection)
     */
    @Query("SELECT * FROM clipboard_items WHERE text = :text LIMIT 1")
    suspend fun findByText(text: String): ClipboardEntity?

    /**
     * Insert a new clipboard item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardEntity)

    /**
     * Update an existing clipboard item
     */
    @Update
    suspend fun update(item: ClipboardEntity)

    /**
     * Delete a single item
     */
    @Delete
    suspend fun delete(item: ClipboardEntity)

    /**
     * Delete item by ID
     */
    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Delete all unpinned items (for "Clear All" functionality)
     */
    @Query("DELETE FROM clipboard_items WHERE isPinned = 0")
    suspend fun deleteAllUnpinned()

    /**
     * Delete all items including pinned ones
     */
    @Query("DELETE FROM clipboard_items")
    suspend fun deleteAll()

    /**
     * Toggle pin status for an item
     */
    @Query("UPDATE clipboard_items SET isPinned = NOT isPinned WHERE id = :id")
    suspend fun togglePin(id: String)

    /**
     * Get total count of items (for FIFO eviction)
     */
    @Query("SELECT COUNT(*) FROM clipboard_items")
    suspend fun getCount(): Int

    /**
     * Get oldest unpinned items (for FIFO eviction)
     */
    @Query(
        """
        SELECT * FROM clipboard_items 
        WHERE isPinned = 0 
        ORDER BY timestamp ASC 
        LIMIT :count
    """,
    )
    suspend fun getOldestUnpinned(count: Int): List<ClipboardEntity>

    /**
     * Move item to top by updating its timestamp (for duplicate handling)
     */
    @Query("UPDATE clipboard_items SET timestamp = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(
        id: String,
        timestamp: Long = System.currentTimeMillis(),
    )
}
