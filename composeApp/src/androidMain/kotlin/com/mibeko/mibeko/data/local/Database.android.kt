package com.mibeko.mibeko.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import com.mibeko.mibeko.MibekoApp

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val context = MibekoApp.INSTANCE.applicationContext
    val dbFile = context.getDatabasePath("mibeko.db")
    return Room.databaseBuilder<AppDatabase>(
        context = context,
        name = dbFile.absolutePath
    )
}
