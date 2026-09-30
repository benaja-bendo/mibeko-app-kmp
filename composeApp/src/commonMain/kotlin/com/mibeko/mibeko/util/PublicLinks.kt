package com.mibeko.mibeko.util

import io.ktor.http.encodeURLParameter
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

    /** Mibeko Apps, l'espace de travail sur ordinateur (D-049). */
    const val MIBEKO_APPS: String = "https://app.mibeko.fr"

    /**
     * Motifs `navDeepLink` de l'écran-relais `TexteResolver` (App.kt). Ils
     * vivent ici, à côté des liens qu'ils doivent ouvrir, pour que les tests
     * vérifient les motifs réels de l'app. L'ordre compte : le motif
     * « article » (plus spécifique) passe avant le motif document seul.
     *
     * Aucun ne déclare de query : la navigation tolère une query non déclarée
     * après le dernier segment et ne la transmet pas aux arguments. C'est ce
     * qui permet à un lien marqué par [shared] d'ouvrir l'app.
     */
    val TEXTE_DEEP_LINK_PATTERNS: List<String> = listOf(
        "$SITE_ORIGIN/textes/{docSlug}/article-{articleNumber}",
        "$SITE_ORIGIN/textes/{docSlug}",
        "mibeko://textes/{docSlug}/article-{articleNumber}",
        "mibeko://textes/{docSlug}",
    )

    /** Format « lien seul » : même valeur que le paramètre `format` de `reader_share`. */
    const val SHARE_FORMAT_LINK: String = "link"

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

    /**
     * Marque [url] comme partagée depuis l'app :
     * `utm_source=app-mibeko&utm_medium=partage&utm_campaign={format}`.
     *
     * WhatsApp et les applications sociales suppriment le référent : sans ces
     * marqueurs, le bouche-à-oreille issu de l'app arrive en « direct » dans
     * Umami (kmp#67). Le site les ignore (canonique et `og:url` sans query),
     * et App Link Android comme Universal Link iOS ne regardent que le chemin :
     * le lien ouvre toujours l'app. À réserver aux partages, sans quoi la mesure
     * compterait aussi les liens que l'app ouvre elle-même.
     */
    fun shared(url: String, format: String): String {
        // Repli sur l'accueil : chemin explicite, la forme `/?…` que les
        // navigateurs affichent de toute façon.
        val base = if (url == SITE_ORIGIN) "$SITE_ORIGIN/" else url
        val separator = if ('?' in base) '&' else '?'
        return "$base${separator}utm_source=app-mibeko&utm_medium=partage" +
            "&utm_campaign=${format.encodeURLParameter()}"
    }
}
