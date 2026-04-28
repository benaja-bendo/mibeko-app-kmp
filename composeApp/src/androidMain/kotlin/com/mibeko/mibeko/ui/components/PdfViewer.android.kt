package com.mibeko.mibeko.ui.components

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mibeko.mibeko.ui.theme.MibekoBluePrimary

@Composable
actual fun PdfViewer(url: String, modifier: Modifier) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showOpenExternally by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        if (showOpenExternally) {
            // Fallback: show button to open in external app
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Impossible d'afficher le PDF dans l'application",
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(url), "application/pdf")
                                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                error = "Aucune application PDF disponible"
                            }
                        }
                    ) {
                        Text("Ouvrir dans une application externe")
                    }
                }
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                error = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                
                                // Check if the page loaded successfully
                                view?.evaluateJavascript("""
                                    (function() {
                                        // Check for Google Docs error messages
                                        var errorElements = document.querySelectorAll('[role="alert"], .error, .warning');
                                        if (errorElements.length > 0) {
                                            return 'error';
                                        }
                                        // Check if PDF viewer loaded
                                        var pdfViewer = document.querySelector('#viewer, .pdfViewer, [data-test-id="pdf-viewer"]');
                                        if (pdfViewer) {
                                            return 'success';
                                        }
                                        return 'unknown';
                                    })()
                                """.trimIndent()) { result ->
                                    if (result?.contains("error") == true) {
                                        showOpenExternally = true
                                    }
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?,
                                errorResponse: android.webkit.WebResourceError?
                            ) {
                                super.onReceivedError(view, request, errorResponse)
                                isLoading = false
                                showOpenExternally = true
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?,
                                errorResponse: android.webkit.WebResourceResponse?
                            ) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                if (errorResponse?.statusCode?.let { it >= 400 } == true) {
                                    showOpenExternally = true
                                }
                            }
                        }
                        
                        loadEnhancedPdf(url)
                    }
                },
                update = { webView ->
                    webView.loadEnhancedPdf(url)
                }
            )
        }

        if (isLoading && !showOpenExternally) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MibekoBluePrimary)
            }
        }

        error?.let { errorMessage ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

private fun WebView.loadEnhancedPdf(url: String) {
    try {
        // Enhanced Google Docs Viewer with better error handling
        val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
        val googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
        
        // Load with additional headers to handle authentication if needed
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language" to "fr-FR,fr;q=0.9,en;q=0.8"
        )
        
        loadUrl(googleDocsUrl, headers)
    } catch (e: Exception) {
        // If all else fails, show external option
        // This will be handled by the WebViewClient error callbacks
    }
}