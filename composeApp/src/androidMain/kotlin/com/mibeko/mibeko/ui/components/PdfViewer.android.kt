package com.mibeko.mibeko.ui.components

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun PdfViewer(url: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                // Use Google Docs viewer to render PDF in WebView on Android
                val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                val pdfUrl = "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
                loadUrl(pdfUrl)
            }
        },
        update = { webView ->
            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
            val pdfUrl = "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
            webView.loadUrl(pdfUrl)
        }
    )
}
