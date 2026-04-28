package com.mibeko.mibeko.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PdfViewer(url: String, modifier: Modifier = Modifier)
