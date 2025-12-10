package io.transcend.samplesdk

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import io.transcend.webview.TranscendConstants

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val ewv = findViewById<WebView>(R.id.appWebView)
        ewv.settings.javaScriptEnabled = true
        ewv.settings.domStorageEnabled = true
        ewv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                ewv.evaluateJavascript(
                    String.format("localStorage.getItem('%s')", TranscendConstants.STORAGE_CONSENT_KEY),
                    System.out::println
                )
                ewv.evaluateJavascript(
                    String.format("localStorage.getItem('%s')", TranscendConstants.STORAGE_TCF_KEY),
                    System.out::println
                )
            }
        }
        ewv.loadUrl("https://docs.transcend.io/docs/consent-management/mobile-consent/android")
        ewv.visibility = View.VISIBLE
    }
}

