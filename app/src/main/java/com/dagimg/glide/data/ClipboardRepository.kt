package com.dagimg.glide.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Repository for clipboard data operations.
 * Handles business logic like duplicate detection, FIFO eviction, and image storage.
 */
class ClipboardRepository(
    context: Context,
) {
    companion object {
        private const val TAG = "ClipboardRepository"
        private const val MAX_ITEMS = 50
        private const val IMAGES_DIR = "clipboard_images"
    }

    private val dao: ClipboardDao = ClipboardDatabase.getInstance(context).clipboardDao()
    private val imagesDir: File = File(context.filesDir, IMAGES_DIR).apply { mkdirs() }

    /**
     * Get all clipboard items as a Flow (reactive updates)
     */
    fun getAllItems(): Flow<List<ClipboardEntity>> = dao.getAllOrdered()

    /**
     * Add text to clipboard history.
     * Handles duplicate detection and FIFO eviction.
     */
    suspend fun addText(
        text: String,
        sourceApp: String? = null,
    ): Boolean {
        if (text.isBlank()) return false

        // Check for duplicate
        val existing = dao.findByText(text)
        if (existing != null) {
            // Move existing item to top by updating timestamp
            dao.updateTimestamp(existing.id)
            Log.d(TAG, "Duplicate text found, moved to top")
            return false
        }

        // Enforce FIFO eviction
        enforceMaxItems()

        val item =
            ClipboardEntity(
                text = text,
                sourceApp = sourceApp,
            )
        dao.insert(item)
        Log.d(TAG, "Added text clip: ${text.take(50)}...")
        return true
    }

    /**
     * Add image to clipboard history.
     * Saves image to internal storage and stores path in database.
     */
    suspend fun addImage(
        bitmap: Bitmap,
        sourceApp: String? = null,
    ): Boolean {
        try {
            // Enforce FIFO eviction
            enforceMaxItems()

            // Save image to internal storage
            val fileName = "clip_${UUID.randomUUID()}.png"
            val imageFile = File(imagesDir, fileName)

            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }

            val item =
                ClipboardEntity(
                    imagePath = imageFile.absolutePath,
                    sourceApp = sourceApp,
                )
            dao.insert(item)
            Log.d(TAG, "Added image clip: $fileName")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save image", e)
            return false
        }
    }

    /**
     * Toggle pin status for an item
     */
    suspend fun togglePin(id: String) {
        dao.togglePin(id)
    }

    /**
     * Delete a single item
     */
    suspend fun delete(item: ClipboardEntity) {
        // Delete image file if exists
        item.imagePath?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete image file", e)
            }
        }
        dao.delete(item)
    }

    /**
     * Delete all unpinned items
     */
    suspend fun clearAllUnpinned() {
        // Get all unpinned items to delete their images
        val items = dao.getOldestUnpinned(Int.MAX_VALUE)
        items.forEach { item ->
            item.imagePath?.let { path ->
                try {
                    File(path).delete()
                } catch (_: Exception) {
                }
            }
        }
        dao.deleteAllUnpinned()
    }

    /**
     * Enforce maximum items limit using FIFO eviction (only unpinned items)
     */
    private suspend fun enforceMaxItems() {
        val count = dao.getCount()
        if (count >= MAX_ITEMS) {
            val toDelete = count - MAX_ITEMS + 1
            val oldestItems = dao.getOldestUnpinned(toDelete)
            oldestItems.forEach { item ->
                item.imagePath?.let { path ->
                    try {
                        File(path).delete()
                    } catch (_: Exception) {
                    }
                }
                dao.delete(item)
            }
            Log.d(TAG, "Evicted $toDelete old items")
        }
    }
}
