package com.dagimg.glide.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a clipboard item stored in the database.
 * Supports both text and image content.
 */
@Entity(tableName = "clipboard_items")
data class ClipboardEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val text: String? = null, // Text content (nullable for images)
    val imagePath: String? = null, // Local file path for images
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val sourceApp: String? = null, // Optional: which app the clipboard came from
) {
    /**
     * Returns true if this is a text-only clipboard item
     */
    val isText: Boolean get() = text != null && imagePath == null

    /**
     * Returns true if this is an image clipboard item
     */
    val isImage: Boolean get() = imagePath != null

    /**
     * Returns a preview string for display (truncated text or "[Image]")
     */
    fun getPreview(maxLength: Int = 100): String =
        when {
            isImage -> "[Image]"
            text != null -> if (text.length > maxLength) text.take(maxLength) + "…" else text
            else -> "[Empty]"
        }
}
