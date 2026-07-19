package com.mibeko.mibeko.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/*
 * Migrations Room réelles pour la base locale `mibeko.db`.
 *
 * Le schéma applicatif est aujourd'hui en v9. Les schémas historiques 1..9 sont
 * tous exportés dans `composeApp/schemas/…AppDatabase/` : chaque migration ci-dessous
 * a été dérivée du *diff* exact entre deux versions consécutives de ces JSON, pas
 * d'une reconstruction approximative. Objectif : PLUS AUCUNE perte de données
 * locales lors d'une mise à jour depuis n'importe quel schéma publié (≥ v1).
 *
 * Plancher retenu : v1. Toute base installée (la toute première version exportée
 * est v1) peut migrer sans destruction jusqu'à v9. Le fallback destructif GLOBAL
 * a été retiré ; seul un *downgrade* (utilisateur qui réinstalle une version plus
 * ancienne de l'app) reste destructif — cas non récupérable proprement et hors
 * périmètre d'une mise à jour normale (voir Database.android.kt / Database.ios.kt).
 *
 * Détail important sur la table FTS `articles_fts` (external content = `articles`) :
 * Room drope TOUS les triggers de synchro FTS dans `onPreMigrate` puis les recrée
 * dans `onPostMigrate`, autour de l'exécution de ces migrations. Nos migrations
 * n'ont donc PAS à gérer ces triggers ; elles peuvent reconstruire librement la
 * table `articles`. On reconstruit néanmoins l'index FTS après un rebuild de
 * `articles` (les rowid changent) via la commande FTS4 'rebuild'.
 */

/**
 * v1 → v2 : aucun changement de schéma.
 *
 * Les schémas 1 et 2 partagent le même `identityHash` (a30393dd…) : seule la
 * version a été incrémentée (probablement un bump manuel sans modification
 * d'entité). Migration volontairement vide — elle existe uniquement pour offrir
 * un chemin de migration non destructif à une base restée en v1.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        // No-op : schémas v1 et v2 identiques.
    }
}

/**
 * v2 → v3 :
 * - `documents` gagne `is_downloaded` (INTEGER NOT NULL) : indicateur « texte
 *   téléchargé pour consultation hors-ligne ». Valeur par défaut 0 (non
 *   téléchargé) pour les lignes existantes — sémantiquement correct.
 * - Nouvelles tables `dossiers` et `dossier_articles` (fonctionnalité dossiers).
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `documents` ADD COLUMN `is_downloaded` INTEGER NOT NULL DEFAULT 0"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `dossiers` " +
                "(`id` TEXT NOT NULL, `name` TEXT NOT NULL, `legal_domain` TEXT NOT NULL, " +
                "`tag` TEXT NOT NULL, `description` TEXT, `color` TEXT NOT NULL, " +
                "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_dossiers_name` ON `dossiers` (`name`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_dossiers_updated_at` ON `dossiers` (`updated_at`)"
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `dossier_articles` " +
                "(`dossier_id` TEXT NOT NULL, `article_id` TEXT NOT NULL, " +
                "`personal_note` TEXT, `added_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`dossier_id`, `article_id`), " +
                "FOREIGN KEY(`dossier_id`) REFERENCES `dossiers`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`article_id`) REFERENCES `articles`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_dossier_articles_dossier_id` ON `dossier_articles` (`dossier_id`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_dossier_articles_article_id` ON `dossier_articles` (`article_id`)"
        )
    }
}

/**
 * v3 → v4 :
 * - `articles.content` passe de NOT NULL à nullable (un article peut être connu
 *   par son numéro sans que son texte soit téléchargé). SQLite ne sait pas
 *   relâcher un NOT NULL via ALTER : on reconstruit la table.
 *
 * On préserve TOUS les `id` d'articles → aucune ligne `dossier_articles`
 * (FK vers `articles`) ne devient orpheline. Les triggers FTS étant retirés par
 * Room avant migration (onPreMigrate) puis recréés après (onPostMigrate), on ne
 * les manipule pas ; on demande juste un rebuild de l'index FTS car les rowid
 * d'`articles` changent après la reconstruction.
 *
 * Note : `articles_fts.content` passe aussi NOT NULL → nullable, mais Room ne
 * valide pas les contraintes NOT NULL d'une table FTS virtuelle (FtsTableInfo ne
 * compare que le jeu de colonnes et les options) : aucune action requise dessus.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `articles_new` " +
                "(`id` TEXT NOT NULL, `node_id` TEXT NOT NULL, `number` TEXT NOT NULL, " +
                "`content` TEXT, `is_favorite` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`node_id`) REFERENCES `nodes`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        connection.execSQL(
            "INSERT INTO `articles_new` (`id`, `node_id`, `number`, `content`, `is_favorite`) " +
                "SELECT `id`, `node_id`, `number`, `content`, `is_favorite` FROM `articles`"
        )
        connection.execSQL("DROP TABLE `articles`")
        connection.execSQL("ALTER TABLE `articles_new` RENAME TO `articles`")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_articles_node_id` ON `articles` (`node_id`)"
        )
        // Les rowid d'`articles` ont changé : on reconstruit l'index FTS externe.
        connection.execSQL("INSERT INTO `articles_fts`(`articles_fts`) VALUES('rebuild')")
    }
}

/**
 * v4 → v5 :
 * - `articles` gagne `is_offline` (INTEGER NOT NULL) : marque un article
 *   explicitement rendu disponible hors-ligne. Défaut 0 pour l'existant.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `articles` ADD COLUMN `is_offline` INTEGER NOT NULL DEFAULT 0"
        )
    }
}

/**
 * v5 → v6 : aucun changement de schéma.
 *
 * Les schémas 5 et 6 partagent le même `identityHash` (30f3342b…) : bump de
 * version seul. Migration vide pour garantir un chemin non destructif.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // No-op : schémas v5 et v6 identiques.
    }
}

/**
 * v6 → v7 :
 * - `documents` gagne `institution_name` (TEXT nullable) et `date_signature`
 *   (TEXT nullable) : métadonnées d'affichage remplies à la synchro du catalogue.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `documents` ADD COLUMN `institution_name` TEXT DEFAULT NULL"
        )
        connection.execSQL(
            "ALTER TABLE `documents` ADD COLUMN `date_signature` TEXT DEFAULT NULL"
        )
    }
}

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

/**
 * v8 → v9 :
 * - `documents` gagne une colonne `slug` (nullable) : clé d'URL publique
 *   `mibeko.fr/textes/{slug}` exposée par l'API, mémorisée pour générer les
 *   liens de partage même hors-ligne. Colonne facultative, aucun backfill —
 *   elle se remplit à la prochaine synchronisation du catalogue.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `documents` ADD COLUMN `slug` TEXT DEFAULT NULL")
    }
}

/** Migrations à enregistrer sur chaque plateforme lors de la construction de la base. */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9
)
