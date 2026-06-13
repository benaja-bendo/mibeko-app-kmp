package com.mibeko.mibeko.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v7 → v8 :
 * - `dossier_articles` perd sa clé étrangère vers `articles` : un lien de
 *   dossier est une donnée utilisateur synchronisée, il doit survivre au
 *   nettoyage du cache d'articles et pouvoir référencer un article non
 *   téléchargé (ajouté depuis un autre appareil).
 * - Nouvelle table `pending_dossier_deletions` pour propager les suppressions
 *   de dossiers au serveur lors de la prochaine synchronisation.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_dossier_deletions` " +
                "(`id` TEXT NOT NULL, `deletedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `dossier_articles_new` " +
                "(`dossier_id` TEXT NOT NULL, `article_id` TEXT NOT NULL, " +
                "`personal_note` TEXT, `added_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`dossier_id`, `article_id`), " +
                "FOREIGN KEY(`dossier_id`) REFERENCES `dossiers`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        connection.execSQL(
            "INSERT OR IGNORE INTO `dossier_articles_new` " +
                "SELECT `dossier_id`, `article_id`, `personal_note`, `added_at` FROM `dossier_articles`"
        )
        connection.execSQL("DROP TABLE `dossier_articles`")
        connection.execSQL("ALTER TABLE `dossier_articles_new` RENAME TO `dossier_articles`")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_dossier_articles_dossier_id` ON `dossier_articles` (`dossier_id`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_dossier_articles_article_id` ON `dossier_articles` (`article_id`)"
        )
    }
}

/** Migrations à enregistrer sur chaque plateforme lors de la construction de la base. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_7_8)
