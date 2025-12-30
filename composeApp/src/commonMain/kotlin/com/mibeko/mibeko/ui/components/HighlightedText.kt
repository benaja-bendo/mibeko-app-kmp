package com.mibeko.mibeko.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * Text component that highlights search keywords within content.
 * Keywords are displayed in bold with primary color accent.
 */
@Composable
fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 3,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    val highlightColor = MaterialTheme.colorScheme.primary
    val normalColor = MaterialTheme.colorScheme.onSurface
    
    val annotatedString = buildAnnotatedString {
        if (query.isBlank()) {
            append(text)
            return@buildAnnotatedString
        }
        
        val queryWords = query.split(" ")
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
        
        var currentIndex = 0
        val lowerText = text.lowercase()
        
        // Find all matches and sort by position
        val matches = mutableListOf<Pair<Int, Int>>()
        for (word in queryWords) {
            var searchStart = 0
            while (true) {
                val foundIndex = lowerText.indexOf(word, searchStart)
                if (foundIndex == -1) break
                matches.add(Pair(foundIndex, foundIndex + word.length))
                searchStart = foundIndex + 1
            }
        }
        
        // Sort and merge overlapping ranges
        val sortedMatches = matches.sortedBy { it.first }
        val mergedMatches = mutableListOf<Pair<Int, Int>>()
        for (match in sortedMatches) {
            if (mergedMatches.isEmpty()) {
                mergedMatches.add(match)
            } else {
                val last = mergedMatches.last()
                if (match.first <= last.second) {
                    mergedMatches[mergedMatches.lastIndex] = Pair(last.first, maxOf(last.second, match.second))
                } else {
                    mergedMatches.add(match)
                }
            }
        }
        
        // Build annotated string
        for (match in mergedMatches) {
            if (currentIndex < match.first) {
                append(text.substring(currentIndex, match.first))
            }
            pushStyle(SpanStyle(
                color = highlightColor,
                fontWeight = FontWeight.Bold
            ))
            append(text.substring(match.first, match.second))
            pop()
            currentIndex = match.second
        }
        
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
    
    Text(
        text = annotatedString,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = normalColor
        )
    )
}
