package com.mibeko.mibeko.data.local

import androidx.room.*
import com.mibeko.mibeko.data.local.dao.MibekoDao
import com.mibeko.mibeko.data.local.entities.*

@Database(
    entities = [
        DocumentEntity::class,
        NodeEntity::class,
        ArticleEntity::class,
        ArticleFtsEntity::class
    ],
    version = 1
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mibekoDao(): MibekoDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
