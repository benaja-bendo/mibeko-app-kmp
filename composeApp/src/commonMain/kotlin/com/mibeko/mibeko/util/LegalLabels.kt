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
