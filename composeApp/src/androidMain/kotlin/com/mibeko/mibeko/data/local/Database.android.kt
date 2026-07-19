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
    ).addMigrations(*ALL_MIGRATIONS)
        // Plancher de migration : v1. Toutes les transitions 1→…→9 ont une
        // migration réelle (voir Migrations.kt) → aucune perte de données locales
        // lors d'une mise à jour depuis n'importe quel schéma publié. Le fallback
        // destructif GLOBAL a été RETIRÉ (il effaçait la base à chaque montée de
        // version). Seul un *downgrade* — un utilisateur qui réinstalle une version
        // plus ancienne de l'app — reste destructif : Room ne peut pas « démigrer »
        // proprement, et ce n'est pas un chemin de mise à jour normal.
        .fallbackToDestructiveMigrationOnDowngrade(true)
}

