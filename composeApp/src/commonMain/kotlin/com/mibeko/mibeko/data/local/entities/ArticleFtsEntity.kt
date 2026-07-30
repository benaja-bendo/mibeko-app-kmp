package com.mibeko.mibeko.data.local.entities

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * Index plein texte des articles (table externe `content=articles`).
 *
 * Tokenizer `unicode61` avec repli des diacritiques : le corpus juridique
 * français est saturé d'accents et le tokenizer `simple` d'origine indexait
 * « société » et « societe » comme deux termes distincts — une recherche
 * hors-ligne saisie sans accents ne trouvait donc rien.
 *
 * `remove_diacritics=1` et surtout PAS `=2` : la valeur 2 exige SQLite ≥ 3.27,
 * soit Android 11 / API 30, alors que l'app cible minSdk 24 avec le SQLite du
 * système.
 */
@Entity(tableName = "articles_fts")
@Fts4(
    contentEntity = ArticleEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=1"]
)
data class ArticleFtsEntity(
    val number: String,
    val content: String?
)
