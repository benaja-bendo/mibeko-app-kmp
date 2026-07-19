package com.mibeko.mibeko.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask


@OptIn(ExperimentalForeignApi::class)
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val dbFilePath = documentDirectory!!.path!! + "/mibeko.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath,
        factory = { AppDatabaseConstructor.initialize() }
    ).setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver())
     .addMigrations(*ALL_MIGRATIONS)
     // Plancher de migration : v1. Toutes les transitions 1→…→9 ont une migration
     // réelle (voir Migrations.kt) → aucune perte de données locales à la mise à
     // jour depuis n'importe quel schéma publié. Le fallback destructif GLOBAL a
     // été RETIRÉ. Seul un *downgrade* (réinstallation d'une version plus ancienne
     // de l'app) reste destructif, cas non récupérable proprement par Room.
     .fallbackToDestructiveMigrationOnDowngrade(true)
}



