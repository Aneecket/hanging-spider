package com.hangingspider.game.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.hangingspider.game.ui.theme.AppColors
import com.hangingspider.game.ui.theme.AppGradients

enum class LegalDoc(val title: String, val asset: String) {
    PRIVACY("PRIVACY POLICY", "file:///android_asset/legal/privacy.html"),
    TERMS("TERMS OF SERVICE", "file:///android_asset/legal/terms.html")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(doc: LegalDoc, onExit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradients.homeBackground)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(doc.title, style = MaterialTheme.typography.headlineMedium, color = AppColors.GoldBright)
                    },
                    navigationIcon = {
                        TextButton(onClick = onExit) {
                            Text("‹ Back", color = AppColors.Lavender, style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { inner ->
            AndroidView(
                modifier = Modifier.padding(inner).fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(0x00000000)
                        settings.javaScriptEnabled = false
                        settings.allowFileAccess = true
                        loadUrl(doc.asset)
                    }
                },
                update = { it.loadUrl(doc.asset) }
            )
        }
    }
}
