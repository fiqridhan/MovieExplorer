package com.example.movieexplorer.presentation.trailer

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri

@SuppressLint("SetJavaScriptEnabled", "UnusedBoxWithConstraintsScope")
@Composable
fun TrailerScreen(trailerUrl: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    val key = trailerUrl?.toUri()?.getQueryParameter("v")
        ?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
    Column {
        TextButton(onClick = onBack) { Text("← Movie details") }
        if (key == null) {
            Text("Trailer unavailable")
            return@Column
        }
        val embed = "https://www.youtube.com/embed/$key"
        BoxWithConstraints(Modifier
            .fillMaxWidth()
            .weight(1f)) {
            val playerHeight = minOf(maxWidth * 9f / 16f, maxHeight)
            AndroidView(
                factory = {
                    WebView(it).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                if (request.url.host == "www.youtube.com") return false
                                if (request.url.host in setOf(
                                        "www.youtube.com",
                                        "youtube.com",
                                        "youtu.be"
                                    )
                                ) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                                }
                                return true
                            }
                        }
                        loadUrl(embed, mapOf("Referer" to "https://${context.packageName}/"))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(playerHeight)
                    .align(Alignment.Center),
                onRelease = { it.destroy() }
            )
        }
        TextButton(onClick = {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    trailerUrl.toUri()
                )
            )
        }) {
            Text("Open in YouTube")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrailerUnavailablePreview() {
    TrailerScreen(trailerUrl = null, onBack = {})
}
