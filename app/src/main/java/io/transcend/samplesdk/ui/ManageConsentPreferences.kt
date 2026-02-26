package io.transcend.samplesdk.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.transcend.webview.TranscendAPI
import io.transcend.webview.TranscendWebView
import io.transcend.webview.models.TranscendConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.HashMap

@Composable
fun ManageConsentPreferences() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTranscendReady by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Initializing Transcend...") }
    var showWebView by remember { mutableStateOf(false) }
    
    // Initialize Transcend when composable is first created
    LaunchedEffect(Unit) {
        // Check if TranscendAPI is already initialized (e.g., from MainActivity)
        // If webViewInstance is not null, it means Transcend is already ready
        val isAlreadyInitialized = try {
            val field = TranscendAPI::class.java.getDeclaredField("webViewInstance")
            field.isAccessible = true
            field.get(null) != null
        } catch (e: Exception) {
            false
        }
        
        if (isAlreadyInitialized) {
            // Already initialized, set state immediately
            isTranscendReady = true
            statusText = "✓ Transcend Ready"
        } else {
            // Not initialized yet, call init and wait for callback
            val config = createTranscendConfig(isStaging = true)
            
            TranscendAPI.init(context.applicationContext, config) { success, errorDetails ->
                // Ensure state updates happen on the main thread
                // The callback may be invoked from a background thread
                scope.launch(Dispatchers.Main) {
                    isTranscendReady = success
                    statusText = if (success) {
                        "✓ Transcend Ready"
                    } else {
                        "Failed to initialize Transcend: $errorDetails"
                    }
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Consent Manager (Compose)",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isTranscendReady) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        
        if (showWebView) {
            TranscendWebViewComposable(
                onClose = { showWebView = false }
            )
        } else {
            Button(
                onClick = { showWebView = true },
                enabled = isTranscendReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Open Consent Manager")
            }
        }
    }
}

@Composable
fun TranscendWebViewComposable(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val config = remember { createTranscendConfig(isStaging = true) }
    var webViewInstance by remember { mutableStateOf<TranscendWebView?>(null) }
    
    AndroidView(
        factory = { ctx ->
            TranscendWebView(ctx).apply {
                setConfig(config)
                loadUrl()
                
                setOnCloseListener { success, errorDetails, consentDetails ->
                    if (success) {
                        println("Transcend closed successfully")
                    } else {
                        println("Transcend closed with error: $errorDetails")
                    }
                    onClose()
                }
                
                webViewInstance = this
            }
        },
        modifier = Modifier.fillMaxSize()
    )
    
    // Show consent manager once when WebView is created and loaded
    LaunchedEffect(webViewInstance) {
        webViewInstance?.let { webView ->
            // Small delay to ensure WebView is fully loaded
            delay(100)
            webView.showConsentManager(null)
        }
    }
    
    // Cleanup when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
        }
    }
}

private fun createTranscendConfig(isStaging: Boolean): TranscendConfig {
    val agAttributes = HashMap<String, String>().apply {
        put("data-prompt", "1")
        put("data-regime", "US_DNSS")
    }
    
    return TranscendConfig.ConfigBuilder(getConsentUrl(isStaging))
        .defaultAttributes(agAttributes)
        .destroyOnClose(false)
        .autoShowUI(false)
        .mobileAppId("RideshareTest")
        .viewState("AcceptOrRejectAllOrMoreChoices")
        .build()
}

private fun getConsentUrl(isStaging: Boolean): String {
    return if (isStaging) {
        "https://transcend-cdn.com/cm-test/e622c065-89e6-4b3c-b31f-46428a1e7b86/airgap.js"
    } else {
        "https://transcend-cdn.com/cm/e622c065-89e6-4b3c-b31f-46428a1e7b86/airgap.js"
    }
}

