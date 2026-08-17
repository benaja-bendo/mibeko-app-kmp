package com.mibeko.mibeko.util

/**
 * Libellés des feuilles de contenu juridique.
 *
 * Jumeau : `mibeko-front/src/shared/lib/legalLabels.ts` (`articleLeafLabel`).
 * Les deux surfaces lisent le même corpus : un numéro technique doit s'afficher
 * de la même façon sur le poste de travail et sur le téléphone.
 */

private const val DUPLICATE_MARKER = "_doublon_"
private const val TABLE_PREFIX = "TABLEAU_"

/**
 * Numéro d'article tel qu'affiché.
 *
 * L'ingestion suffixe en `_doublon_N` les numéros qui entrent en collision au
 * sein d'un même document, pour satisfaire la contrainte d'unicité en base.
 * Ces collisions sont presque toujours des actes distincts réunis dans un même
 * document, pas de vraies duplications — le suffixe est un artefact de
 * stockage, pas un numéro juridique. Il reste dans la donnée et dans les URL
 * de partage (il identifie l'article) ; seul l'affichage est nettoyé.
 */
fun displayArticleNumber(number: String?): String {
    val value = number ?: return ""
    val marker = value.lastIndexOf(DUPLICATE_MARKER)
    if (marker < 0) return value

    val suffix = value.substring(marker + DUPLICATE_MARKER.length)
    val isCounter = suffix.isNotEmpty() && suffix.all { it in '0'..'9' }
    return if (isCounter) value.substring(0, marker) else value
}

/**
 * Libellé lisible d'une feuille de contenu juridique.
 *
 * Certaines feuilles portent un numéro technique plutôt qu'un vrai numéro
 * d'article : le préambule d'un acte (qualité du signataire, visas « Vu … »,
 * considérants), la formule finale (« Fait à … » + signataire) et les tableaux.
 * Sans ce libellé, l'écran affiche « Article TABLEAU_1 ».
 *
 * @param number Le numéro de la feuille (ex. « 1er », « PREAMBULE », « TABLEAU_2 »).
 * @param short  Forme abrégée pour les listes denses (sommaire) : « Art. 1er ».
 */
fun articleLeafLabel(number: String?, short: Boolean = false): String {
    val value = displayArticleNumber(number).trim()

    if (value == "PREAMBULE") return "Préambule"
    if (value == "SIGNATURE") return "Signature"

    if (value.startsWith(TABLE_PREFIX)) {
        val rank = value.substring(TABLE_PREFIX.length)
        if (rank.isNotEmpty() && rank.all { it in '0'..'9' }) {
            return if (short) "Tab. $rank" else "Tableau $rank"
        }
    }

    return if (short) "Art. $value" else "Article $value"
}

/**
 * Intitulé d'un document tel qu'affiché sur UNE seule ligne (liste, résultat de
 * recherche, fil d'Ariane, libellé de partage).
 *
 * Le Journal officiel publie certaines décisions en « actes en abrégé » : son
 * sommaire n'annonce que « Nomination. » et l'en-tête n'imprime aucun objet.
 * L'intitulé du texte est alors littéralement « Décret n° 2025-240 du 20 juin
 * 2025. » — fidèle à la source (vérifié le 16/08/2026 contre les markdowns
 * MinerU), et parfaitement muet. Le libellé descriptif porte l'objet DÉRIVÉ du
 * corps de l'acte pour compenser ce silence.
 *
 * RÈGLE À NE PAS DÉFAIRE : le libellé descriptif n'est PAS le titre officiel.
 * Cette fonction les concatène, elle ne substitue jamais l'un à l'autre — un
 * écran qui n'afficherait que le libellé présenterait comme intitulé officiel
 * une paraphrase qui n'en est pas un.
 *
 * Jumeau : `mibeko-front/src/shared/lib/legalLabels.ts` (`documentLineLabel`).
 */
fun documentLineLabel(officialTitle: String?, descriptiveLabel: String?): String {
    val title = officialTitle?.trim().orEmpty()
    val label = descriptiveLabel?.trim().orEmpty()

    if (title.isEmpty()) {
        return label.ifEmpty { "Document" }
    }

    if (label.isEmpty()) {
        return title
    }

    // Le point final de « … du 20 juin 2025. » ferait une coupure bancale
    // devant le tiret : retiré de l'AFFICHAGE seulement, jamais de la donnée.
    return "${title.trimEnd(' ', '.', ',', ';')} — $label"
}
