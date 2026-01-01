package com.mibeko.mibeko.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android-specific database builder using Koin for context injection.
 */
private object AndroidDatabaseProvider : KoinComponent {
    val context: Context by inject()
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val context = AndroidDatabaseProvider.context
    val dbFile = context.getDatabasePath("mibeko.db")
    return Room.databaseBuilder<AppDatabase>(
        context = context,
        name = dbFile.absolutePath
    ).fallbackToDestructiveMigration(true)
}

