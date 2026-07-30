package com.mibeko.mibeko.data.local

import androidx.room.*
import com.mibeko.mibeko.data.local.dao.MibekoDao
import com.mibeko.mibeko.data.local.entities.*

@Database(
    entities = [
        DocumentEntity::class,
        NodeEntity::class,
        ArticleEntity::class,
        ArticleFtsEntity::class,
        TagEntity::class,
        ArticleTagEntity::class,
        DossierEntity::class,
        DossierArticleEntity::class,
        PendingDossierDeletionEntity::class
    ],
    version = 10
)

@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mibekoDao(): MibekoDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

