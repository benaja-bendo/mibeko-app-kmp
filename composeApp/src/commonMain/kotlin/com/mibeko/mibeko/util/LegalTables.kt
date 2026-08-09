package com.mibeko.mibeko.util

import kotlinx.serialization.Serializable

/**
 * Tableaux d'un article juridique — modèle canonique et segmentation du contenu.
 *
 * Invariant du corpus : `contenu_texte` ne contient jamais de balisage. Un
 * tableau y est **linéarisé** (une ligne par rangée, cellules séparées par
 * « | ») et sa forme structurée voyage à côté.
 *
 * Deux chemins, dans cet ordre de confiance :
 *   1. `tables` synchronisés depuis l'API, ancrés sur les lignes qu'ils
 *      occupent — le cas normal depuis la normalisation du corpus ;
 *   2. repli hérité : du HTML MinerU encore présent dans le texte d'articles
 *      synchronisés avant. Ce second chemin est **transitoire** et disparaîtra
 *      quand plus aucun appareil ne portera de corpus d'avant la bascule.
 *
 * Jumeau faisant autorité : `mibeko-site/src/lib/tables.ts`, dont les cas sont
 * testés dans `mibeko-front/src/shared/lib/tables.test.ts`. Toute correction
 * ici doit être reportée là-bas, et réciproquement.
 *
 * Analyse à la main plutôt qu'avec `Regex` : le moteur d'expressions régulières
 * de Kotlin/Native n'est pas celui de la JVM, et les tests ne tournent que sur
 * l'hôte JVM — une divergence de moteur ne serait vue par personne avant
 * l'écran d'un utilisateur iOS.
 */

/** Tableau sous forme canonique : en-têtes optionnels + rangées de cellules. */
data class LegalTable(
    /**
     * Intitulé du tableau. Toujours `null` par le chemin HTML hérité : MinerU
     * n'émet pas de `<caption>`. Le champ existe pour la forme canonique que le
     * pipeline produira.
     */
    val caption: String? = null,
    /** Cellules de la ligne d'en-tête. Vide si le tableau n'en a pas. */
    val headers: List<String> = emptyList(),
    /** Rangées de données, hors en-tête. */
    val rows: List<List<String>> = emptyList()
)

/**
 * Tableau tel que servi par l'API et stocké dans le corpus hors-ligne.
 *
 * Jumeau de `ApiTable` côté TypeScript. Les bornes sont des indices de lignes
 * de `content` (début inclus, fin exclue) : elles disent quelles lignes du
 * texte linéarisé ce tableau occupe, sans qu'aucun marqueur n'ait à transiter
 * par le texte lui-même.
 */
@Serializable
data class ArticleTable(
    val caption: String? = null,
    val headers: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val line_start: Int? = null,
    val line_end: Int? = null
) {
    fun toLegalTable(): LegalTable = LegalTable(caption = caption, headers = headers, rows = rows)
}

/** Morceau de contenu à rendre : du texte, ou un tableau. */
sealed interface ContentSegment {
    data class Text(val text: String) : ContentSegment

    data class Table(val table: LegalTable) : ContentSegment
}

/** Entités HTML rencontrées dans les cellules produites par MinerU. */
private val NAMED_ENTITIES = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to " ",
    "laquo" to "«",
    "raquo" to "»",
    "deg" to "°",
    "eacute" to "é",
    "egrave" to "è",
    "agrave" to "à",
    "ccedil" to "ç"
)

/** Balises de tableau : celles dont un reliquat ne doit jamais atteindre l'écran. */
private val TABLE_TAGS = listOf("table", "tr", "td", "th")

// =============================================================================
// Point d'entrée des surfaces de lecture
// =============================================================================

/**
 * Segmente le contenu d'un article pour l'affichage.
 *
 * Les surfaces n'ont pas à savoir si l'article a déjà été normalisé : sans
 * balise de tableau, le contenu ressort en un unique segment de texte, intact.
 */
fun articleSegments(content: String?, tables: List<ArticleTable> = emptyList()): List<ContentSegment> {
    if (content.isNullOrEmpty()) return emptyList()
    if (tables.isNotEmpty()) return segmentsFromTables(content, tables)
    if (!hasRawTableMarkup(content)) return textSegment(content)
    return segmentsFromHtml(content)
}

/**
 * Segmente le contenu à partir des tableaux structurés servis par l'API.
 *
 * Une borne absente ou incohérente fait retomber le tableau en fin de contenu
 * plutôt que de tronquer le texte : mieux vaut un tableau mal placé qu'un texte
 * officiel amputé.
 */
fun segmentsFromTables(content: String, tables: List<ArticleTable>): List<ContentSegment> {
    val lines = content.split("\n")
    val ancres = tables
        .filter { it.line_start != null && it.line_end != null && it.line_start >= 0 && it.line_end > it.line_start }
        .sortedBy { it.line_start }

    val segments = mutableListOf<ContentSegment>()
    var cursor = 0

    for (table in ancres) {
        val start = minOf(table.line_start!!, lines.size)
        val end = minOf(table.line_end!!, lines.size)
        if (start < cursor) continue // Chevauchement : on garde le premier.
        segments += textSegment(lines.subList(cursor, start).joinToString("\n"))
        segments += ContentSegment.Table(table.toLegalTable())
        cursor = end
    }

    segments += textSegment(lines.subList(cursor, lines.size).joinToString("\n"))

    // Tableaux sans ancrage exploitable : rendus à la suite, jamais perdus.
    tables.filterNot { it in ancres }.forEach { segments += ContentSegment.Table(it.toLegalTable()) }

    return segments
}

/**
 * Segmente un contenu hérité contenant du HTML MinerU brut.
 *
 * Le texte hors tableaux est conservé tel quel : un article porte souvent une
 * phrase d'introduction avant son tableau de coordonnées.
 */
fun segmentsFromHtml(content: String): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()
    var cursor = 0

    while (true) {
        val open = findOpeningTag(content, "table", cursor) ?: break
        val close = findClosingTag(content, "table", open.contentStart) ?: break

        segments += textSegment(content.substring(cursor, open.start))

        val rows = parseTableRows(content.substring(open.contentStart, close.first))
        if (rows.isEmpty()) {
            // `<table>` sans rangée exploitable : le fragment est rendu tel quel
            // plutôt qu'escamoté — mieux vaut du balisage visible, donc
            // signalable, qu'un texte officiel amputé en silence.
            segments += textSegment(content.substring(open.start, close.last + 1))
        } else {
            val hasHeader = rows.size > 1 && looksLikeHeaderRow(rows[0])
            segments += ContentSegment.Table(
                LegalTable(
                    caption = null,
                    headers = if (hasHeader) rows[0] else emptyList(),
                    rows = if (hasHeader) rows.drop(1) else rows
                )
            )
        }

        cursor = close.last + 1
    }

    segments += textSegment(content.substring(cursor))

    return segments
}

/** Vrai si le contenu porte encore du balisage de tableau non normalisé. */
fun hasRawTableMarkup(content: String?): Boolean =
    content != null && containsTag(content, "table")

/**
 * Rendu textuel d'un tableau : une ligne par rangée, cellules séparées par « | ».
 *
 * C'est la forme que le pipeline écrit dans `contenu_texte`, et celle qu'on
 * réutilise partout où seul du texte peut passer (presse-papier, partage,
 * aperçu). Elle doit rester identique à la linéarisation amont
 * (`mibeko-python/src/extractor/tables.py`).
 */
fun linearizeTable(table: LegalTable): String {
    val lines = mutableListOf<String>()
    table.caption?.takeIf { it.isNotEmpty() }?.let { lines += it }
    if (table.headers.isNotEmpty()) lines += table.headers.joinToString(" | ")
    table.rows.forEach { row -> lines += row.joinToString(" | ") }
    return lines.joinToString("\n")
}

/**
 * Contenu d'un article ramené à du texte pur, tableaux linéarisés.
 *
 * Garantie tenue par cette fonction : aucune balise n'en sort. C'est ce qui
 * part dans le presse-papier et dans le partage, où du HTML serait à la fois
 * illisible et faux.
 */
fun articlePlainText(content: String?, tables: List<ArticleTable> = emptyList()): String =
    articleSegments(content, tables).joinToString("\n") { segment ->
        when (segment) {
            is ContentSegment.Text -> readableText(segment.text)
            is ContentSegment.Table -> linearizeTable(segment.table)
        }
    }.trim()

/**
 * Texte d'un segment débarrassé d'un éventuel reliquat de balisage.
 *
 * Un `<table>` jamais refermé reste volontairement dans le texte à la
 * segmentation (ne rien perdre prime), mais il n'a sa place ni à l'écran ni
 * dans le presse-papier. Le nettoyage ne se déclenche donc qu'en présence de
 * balisage de tableau : ailleurs, « < » est un vrai caractère et les retours à
 * la ligne portent le découpage en alinéas.
 *
 * Écart assumé avec le jumeau TypeScript, qui laisse ce reliquat passer : là-bas
 * il finit dans une description SEO, ici sous les yeux d'un lecteur.
 */
fun readableText(text: String): String {
    if (TABLE_TAGS.none { containsTag(text, it) }) return text
    return decodeEntities(stripTags(text))
        .split('\n')
        .joinToString("\n") { line -> collapseSpaces(line) }
        .trim()
}

// =============================================================================
// Analyse du HTML hérité
// =============================================================================

/**
 * Une rangée est-elle une ligne d'en-tête ?
 *
 * MinerU n'émet pas de `<th>` : il faut deviner. Critère volontairement étroit —
 * chaque cellule non vide doit contenir au moins une lettre. Une rangée de
 * données commence presque toujours par un identifiant numérique (« 3-2-1 ») ou
 * un montant, ce qui la disqualifie. Le pire cas est cosmétique (une rangée
 * affichée en gras) ; la structuration amont, elle, tranche pour de bon.
 */
private fun looksLikeHeaderRow(cells: List<String>): Boolean {
    val filled = cells.filter { it.isNotEmpty() }
    return filled.isNotEmpty() && filled.all { cell -> cell.any { it.isLetter() } }
}

/** Parse un fragment `<table>…</table>` en rangées de texte brut. */
private fun parseTableRows(inner: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var cursor = 0

    while (true) {
        val open = findOpeningTag(inner, "tr", cursor) ?: break
        val close = findClosingTag(inner, "tr", open.contentStart) ?: break
        val cells = parseRowCells(inner.substring(open.contentStart, close.first))
        if (cells.isNotEmpty()) rows += cells
        cursor = close.last + 1
    }

    return rows
}

/** Parse un fragment `<tr>…</tr>` en cellules de texte brut. */
private fun parseRowCells(row: String): List<String> {
    val cells = mutableListOf<String>()
    var cursor = 0

    while (true) {
        val dataCell = findOpeningTag(row, "td", cursor)
        val headerCell = findOpeningTag(row, "th", cursor)
        // MinerU mêle `<td>` et `<th>` dans une même rangée : on prend la
        // prochaine cellule dans l'ordre du texte, quel que soit son nom.
        val useDataCell = dataCell != null && (headerCell == null || dataCell.start < headerCell.start)
        val open = (if (useDataCell) dataCell else headerCell) ?: break
        val name = if (useDataCell) "td" else "th"

        val close = findClosingTag(row, name, open.contentStart) ?: break
        cells += cellText(row.substring(open.contentStart, close.first))

        // `colspan` est aplati en cellules vides pour préserver l'alignement des
        // colonnes des rangées suivantes. `rowspan` n'est pas propagé : la
        // structuration amont le signale (`tableau_suspect`) plutôt que de
        // deviner à l'affichage.
        var extra = colspanOf(open.attributes)
        while (extra > 1) {
            cells += ""
            extra--
        }

        cursor = close.last + 1
    }

    return cells
}

/** Texte d'une cellule : balises internes retirées, entités décodées, espaces normalisés. */
private fun cellText(html: String): String = collapseSpaces(decodeEntities(stripTags(html)))

private fun textSegment(text: String): List<ContentSegment> =
    if (text.all { it.isCollapsibleSpace() }) emptyList() else listOf(ContentSegment.Text(text))

/**
 * Portée horizontale d'une cellule (`colspan`).
 *
 * Au-delà de 32, l'attribut est du bruit OCR et non une intention de mise en
 * forme : la cellule compte pour une plutôt que de gonfler la rangée de
 * milliers de cellules vides. Même borne dans les deux jumeaux
 * (`mibeko-site/src/lib/tables.ts`, `mibeko-python/src/extractor/tables.py`) ;
 * le pipeline, lui, en fait une anomalie de curation.
 */
private fun colspanOf(attributes: String): Int {
    val at = attributes.indexOf("colspan", ignoreCase = true)
    if (at < 0) return 1

    var cursor = skipSpaces(attributes, at + "colspan".length)
    if (cursor >= attributes.length || attributes[cursor] != '=') return 1
    cursor = skipSpaces(attributes, cursor + 1)
    if (cursor < attributes.length && (attributes[cursor] == '"' || attributes[cursor] == '\'')) cursor++

    val start = cursor
    while (cursor < attributes.length && attributes[cursor] in '0'..'9') cursor++
    if (cursor == start) return 1

    val value = attributes.substring(start, cursor).toIntOrNull() ?: return 1
    return if (value in 2..32) value else 1
}

// =============================================================================
// Balayage de balises (équivalent des expressions régulières du jumeau)
// =============================================================================

/** Balise ouvrante repérée : bornes dans la chaîne + attributs bruts. */
private class HtmlTag(val start: Int, val contentStart: Int, val attributes: String)

/**
 * Prochaine balise ouvrante `<nom …>` à partir de `from`.
 *
 * La frontière de mot du jumeau (`<table\b`) est reproduite : `<tableau>` ne
 * doit pas passer pour un `<table>`.
 */
private fun findOpeningTag(content: String, name: String, from: Int): HtmlTag? {
    var index = from
    while (true) {
        val at = content.indexOf("<$name", startIndex = index, ignoreCase = true)
        if (at < 0) return null

        val afterName = at + 1 + name.length
        val close = content.indexOf('>', afterName)
        if (close < 0) return null

        if (afterName >= content.length || !isWordChar(content[afterName])) {
            return HtmlTag(start = at, contentStart = close + 1, attributes = content.substring(afterName, close))
        }
        index = at + 1
    }
}

/** Prochaine balise fermante `</nom >` à partir de `from` ; bornes incluses. */
private fun findClosingTag(content: String, name: String, from: Int): IntRange? {
    var index = from
    while (true) {
        val at = content.indexOf("</$name", startIndex = index, ignoreCase = true)
        if (at < 0) return null

        val cursor = skipSpaces(content, at + 2 + name.length)
        if (cursor < content.length && content[cursor] == '>') return at..cursor
        index = at + 1
    }
}

/** Le contenu porte-t-il une balise ouvrante de ce nom ? */
private fun containsTag(content: String, name: String): Boolean {
    var index = 0
    while (true) {
        val at = content.indexOf("<$name", startIndex = index, ignoreCase = true)
        if (at < 0) return false
        val afterName = at + 1 + name.length
        if (afterName >= content.length || !isWordChar(content[afterName])) return true
        index = at + 1
    }
}

/** Retire les balises, chacune remplacée par une espace (jamais recollée aux mots voisins). */
private fun stripTags(html: String): String {
    if (!html.contains('<')) return html

    val out = StringBuilder(html.length)
    var index = 0
    while (index < html.length) {
        val char = html[index]
        if (char != '<') {
            out.append(char)
            index++
            continue
        }
        val close = html.indexOf('>', index + 1)
        if (close < 0) {
            // « < » sans « > » : c'est un vrai caractère du texte, pas une balise.
            out.append(char)
            index++
        } else {
            out.append(' ')
            index = close + 1
        }
    }
    return out.toString()
}

// =============================================================================
// Entités et espaces
// =============================================================================

/**
 * Décode les entités HTML d'un fragment.
 *
 * Une séquence non reconnue (`&Vu ;`, `&inconnu;`) ressort telle quelle : le
 * texte est officiel, on ne devine pas à sa place.
 */
internal fun decodeEntities(input: String): String {
    if (!input.contains('&')) return input

    val out = StringBuilder(input.length)
    var index = 0
    while (index < input.length) {
        val char = input[index]
        if (char != '&') {
            out.append(char)
            index++
            continue
        }

        val end = input.indexOf(';', index + 1)
        val decoded = if (end > index + 1) decodeEntityBody(input.substring(index + 1, end)) else null
        if (decoded == null) {
            out.append(char)
            index++
        } else {
            out.append(decoded)
            index = end + 1
        }
    }
    return out.toString()
}

private fun decodeEntityBody(body: String): String? {
    if (body[0] != '#') {
        return if (body.all { isAsciiLetter(it) }) NAMED_ENTITIES[body.lowercase()] else null
    }

    val hexadecimal = body.length > 1 && (body[1] == 'x' || body[1] == 'X')
    val digits = body.substring(if (hexadecimal) 2 else 1)
    if (digits.isEmpty()) return null

    val code = digits.toIntOrNull(if (hexadecimal) 16 else 10) ?: return null
    if (code <= 0 || code > 0x10FFFF) return null
    return codePointToString(code)
}

private fun codePointToString(code: Int): String {
    if (code <= 0xFFFF) return code.toChar().toString()
    val offset = code - 0x10000
    return charArrayOf(
        (0xD800 + (offset shr 10)).toChar(),
        (0xDC00 + (offset and 0x3FF)).toChar()
    ).concatToString()
}

/** Réduit les suites d'espaces à une seule et supprime celles des extrémités. */
private fun collapseSpaces(input: String): String {
    val out = StringBuilder(input.length)
    var pending = false
    for (char in input) {
        if (char.isCollapsibleSpace()) {
            pending = out.isNotEmpty()
            continue
        }
        if (pending) {
            out.append(' ')
            pending = false
        }
        out.append(char)
    }
    return out.toString()
}

private fun skipSpaces(content: String, from: Int): Int {
    var cursor = from
    while (cursor < content.length && content[cursor].isCollapsibleSpace()) cursor++
    return cursor
}

/**
 * Espace au sens du `\s` JavaScript, que le jumeau utilise pour normaliser.
 *
 * `Char.isWhitespace()` ne classe pas l'espace insécable de la même façon selon
 * la plateforme (faux sur la JVM, vrai sur Native) : la lister ici évite un
 * rendu différent entre Android et iOS.
 */
private fun Char.isCollapsibleSpace(): Boolean =
    isWhitespace() || this == '\u00A0' || this == '\uFEFF'

private fun isAsciiLetter(char: Char): Boolean = char in 'a'..'z' || char in 'A'..'Z'

private fun isWordChar(char: Char): Boolean = char.isLetterOrDigit() || char == '_'
