package com.mibeko.mibeko.util

import io.ktor.http.encodeURLPathPart

/**
 * Construction des liens publics vers le portail citoyen mibeko.fr.
 *
 * Le lecteur public vit sous `/textes/{slug}` (document) et
 * `/textes/{slug}/article-{numero}` (article). Le format DOIT rester aligné
 * sur la route Astro `src/pages/textes/[doc]/[article].astro` (helper
 * `articlePath` : `/textes/${slug}/article-${encodeURIComponent(number)}`) et
 * sur l'App Link Android (host `mibeko.fr`, `pathPrefix=/textes`).
 *
 * Quand le slug est inconnu (document jamais publié, coquille locale issue
 * d'une recherche), on retombe proprement sur la page d'accueil du portail
 * plutôt que de fabriquer une URL cassée.
 */
object PublicLinks {

    /** Origine du portail citoyen. Le domaine réel est mibeko.fr (pas .cg). */
    const val SITE_ORIGIN: String = "https://mibeko.fr"

    /**
     * Lien public d'un document. Retombe sur [SITE_ORIGIN] si le slug manque.
     */
    fun document(slug: String?): String {
        val safe = slug?.trim().orEmpty()
        return if (safe.isEmpty()) SITE_ORIGIN
        else "$SITE_ORIGIN/textes/${safe.encodeURLPathPart()}"
    }

    /**
     * Lien public d'un article : `/textes/{slug}/article-{numero}`.
     *
     * Le numéro d'article est encodé comme segment de chemin (parité avec
     * `encodeURIComponent` côté site). Si le slug du document est inconnu, on
     * retombe sur [SITE_ORIGIN] : sans slug le lecteur ne peut pas résoudre le
     * texte, un lien d'accueil vaut mieux qu'un lien mort.
     */
    fun article(documentSlug: String?, articleNumber: String): String {
        val safeSlug = documentSlug?.trim().orEmpty()
        if (safeSlug.isEmpty()) return SITE_ORIGIN
        val encodedNumber = articleNumber.trim().encodeURLPathPart()
        return "$SITE_ORIGIN/textes/${safeSlug.encodeURLPathPart()}/article-$encodedNumber"
    }
}
