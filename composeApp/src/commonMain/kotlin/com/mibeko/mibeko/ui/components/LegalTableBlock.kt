package com.mibeko.mibeko.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mibeko.mibeko.util.LegalTable

/**
 * Tableau juridique rendu comme un vrai tableau.
 *
 * Défilement horizontal obligatoire : le tableau budgétaire du corpus fait cinq
 * colonnes, qui ne tiennent pas dans les 375 dp d'un téléphone courant. Le geste
 * horizontal est consommé ici, ce qui neutralise volontairement le swipe
 * « article suivant » du lecteur tant que le doigt repose sur le tableau.
 *
 * Les couleurs se dérivent de la couleur de texte du thème de LECTURE (papier /
 * clair / sombre) et non de `MaterialTheme` : c'est la règle du lecteur, seule
 * façon de rester lisible sur les trois fonds.
 */
@Composable
fun LegalTableBlock(
    table: LegalTable,
    textColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    dyslexiaFriendly: Boolean,
    modifier: Modifier = Modifier
) {
    val columnCount = maxOf(table.headers.size, table.rows.maxOfOrNull { it.size } ?: 0)
    if (columnCount == 0) return

    // Largeur de colonne indexée sur la taille de police choisie : figée, une
    // colonne couperait chaque cellule en quatre lignes au réglage « Grand ».
    val columnWidth: Dp = (fontSize.value * 7f).dp
    val tableWidth: Dp = columnWidth * columnCount
    val borderColor = textColor.copy(alpha = 0.15f)
    val shape = RoundedCornerShape(8.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        table.caption?.takeIf { it.isNotBlank() }?.let { caption ->
            Text(
                text = caption,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, shape)
                .clip(shape)
        ) {
            Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                if (table.headers.isNotEmpty()) {
                    TableRow(
                        cells = table.headers,
                        columnCount = columnCount,
                        columnWidth = columnWidth,
                        textColor = textColor,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        dyslexiaFriendly = dyslexiaFriendly,
                        isHeader = true,
                        background = textColor.copy(alpha = 0.09f)
                    )
                    TableSeparator(width = tableWidth, color = borderColor)
                }

                table.rows.forEachIndexed { index, row ->
                    if (index > 0) TableSeparator(width = tableWidth, color = borderColor.copy(alpha = 0.08f))
                    TableRow(
                        cells = row,
                        columnCount = columnCount,
                        columnWidth = columnWidth,
                        textColor = textColor,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        dyslexiaFriendly = dyslexiaFriendly,
                        isHeader = false,
                        // Alternance discrète : sur cinq colonnes, l'œil perd
                        // sa ligne sans repère horizontal.
                        background = if (index % 2 == 1) textColor.copy(alpha = 0.035f) else Color.Transparent
                    )
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    columnCount: Int,
    columnWidth: Dp,
    textColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    dyslexiaFriendly: Boolean,
    isHeader: Boolean,
    background: Color
) {
    Row(modifier = Modifier.background(background)) {
        for (index in 0 until columnCount) {
            val value = cells.getOrElse(index) { "" }
            Text(
                text = value,
                modifier = Modifier
                    .width(columnWidth)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    // Un chiffre de tableau se lit en colonne : à droite, les
                    // ordres de grandeur s'alignent. Un libellé reste à gauche.
                    textAlign = if (isValueCell(value)) TextAlign.End else TextAlign.Start,
                    fontSize = fontSize * 0.85f,
                    lineHeight = lineHeight * 0.75f,
                    letterSpacing = if (dyslexiaFriendly) 0.8.sp else 0.2.sp,
                    fontWeight = when {
                        isHeader -> FontWeight.Bold
                        dyslexiaFriendly -> FontWeight.Medium
                        else -> FontWeight.Normal
                    }
                ),
                color = if (isHeader) textColor else textColor.copy(alpha = 0.9f)
            )
        }
    }
}

/**
 * Filet de séparation.
 *
 * Largeur explicite : dans un conteneur à défilement horizontal, la contrainte
 * de largeur est infinie et `fillMaxWidth` n'a plus de sens.
 */
@Composable
private fun TableSeparator(width: Dp, color: Color) {
    Box(
        modifier = Modifier
            .width(width)
            .height(1.dp)
            .background(color)
    )
}

/**
 * Cellule de valeur (montant, année, quantité) : alignée à droite.
 *
 * Les espaces insécables font partie de la ponctuation admise : ils séparent
 * les milliers dans les montants en FCFA du corpus, et sans eux « 1 250 000 »
 * passerait pour du texte.
 */
private fun isValueCell(text: String): Boolean {
    if (text.isEmpty()) return false

    var hasDigit = false
    for (char in text) {
        when {
            char in '0'..'9' -> hasDigit = true
            char in " .,;:%-+()/'\u00A0\u202F" -> Unit
            else -> return false
        }
    }
    return hasDigit
}
