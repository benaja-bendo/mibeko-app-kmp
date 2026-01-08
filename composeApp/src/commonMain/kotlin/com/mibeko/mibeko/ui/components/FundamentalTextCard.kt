package com.mibeko.mibeko.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mibeko.mibeko.ui.home.FundamentalText

private val MibekoPrimaryBlue = Color(0xFF1A3A6B)
private val MibekoSecondaryBlue = Color(0xFF2E5A9C)
private val MibekoGold = Color(0xFFB8860B)
private val ConstitutionRed = Color(0xFF8B0000) // Deep red for Constitution

/**
 * Card for fundamental legal texts in the horizontal carousel.
 * Zone 2 of the new home page layout.
 */
@Composable
fun FundamentalTextCard(
    text: FundamentalText,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConstitution = text.typeCode == "CONSTITUTION"
    
    val primaryColor = if (isConstitution) ConstitutionRed else MibekoSecondaryBlue
    val gradientColors = if (isConstitution) {
        listOf(ConstitutionRed, Color(0xFFCD5C5C))
    } else {
        listOf(MibekoPrimaryBlue, MibekoSecondaryBlue)
    }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(colors = gradientColors)
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Text(
                    text = text.shortTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Download status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (text.isDownloaded) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)), // Green
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Téléchargé",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "Hors-ligne",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Cloud,
                            contentDescription = "En ligne uniquement",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "En ligne",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
