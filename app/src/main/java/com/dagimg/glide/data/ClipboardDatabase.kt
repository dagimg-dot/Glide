package com.dagimg.glide.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for clipboard history.
 * Uses singleton pattern to ensure single instance across the app.
 */
@Database(
    entities = [ClipboardEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ClipboardDatabase : RoomDatabase() {
    abstract fun clipboardDao(): ClipboardDao

    companion object {
        @Volatile
        private var INSTANCE: ClipboardDatabase? = null

        fun getInstance(context: Context): ClipboardDatabase =
            INSTANCE ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            ClipboardDatabase::class.java,
                            "glide_clipboard.db",
                        ).fallbackToDestructiveMigration()
                        .build()
                INSTANCE = instance
                instance
            }
    }
}
