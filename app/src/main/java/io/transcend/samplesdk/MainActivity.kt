package io.transcend.samplesdk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.transcend.webview.TranscendAPI
import io.transcend.webview.TranscendWebView
import io.transcend.webview.models.TranscendConfig
//import com.google.android.gms.ads.MobileAds
import io.transcend.webview.models.ConsentStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MOBILE_APP_ID = "RideshareTest"
        
        private fun getConsentUrl(isStaging: Boolean): String {
            return if (isStaging) {
                "https://transcend-cdn.com/cm-test/e622c065-89e6-4b3c-b31f-46428a1e7b86/airgap.js"
            } else {
                "https://transcend-cdn.com/cm/e622c065-89e6-4b3c-b31f-46428a1e7b86/airgap.js"
            }
        }
        
        private fun createConfig(isStaging: Boolean): TranscendConfig {
            return TranscendConfig.ConfigBuilder(getConsentUrl(isStaging))
                .destroyOnClose(false)
                .autoShowUI(false)
                .mobileAppId(MOBILE_APP_ID)
                .build()
        }
    }

    private var isTranscendReady = false
    private lateinit var statusLabel: TextView
    private lateinit var transcendButton: Button
    private lateinit var resetButton: Button
    private lateinit var printConsentButton: Button
    private lateinit var composeButton: Button
    private lateinit var consentTextView: TextView
    private var bottomSheetDialog: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupViews()
        configureTranscend()
    }

    private fun setupViews() {
        statusLabel = findViewById(R.id.statusLabel)
        transcendButton = findViewById(R.id.transcendButton)
        resetButton = findViewById(R.id.resetButton)
        printConsentButton = findViewById(R.id.printConsentButton)
        composeButton = findViewById(R.id.composeButton)
        consentTextView = findViewById(R.id.consentTextView)
        
        // Initially disable buttons
        transcendButton.isEnabled = false
        resetButton.isEnabled = false
        printConsentButton.isEnabled = false
        
        // Set click listeners
        transcendButton.setOnClickListener { presentTranscend() }
        resetButton.setOnClickListener { resetTranscend() }
        printConsentButton.setOnClickListener { printConsentInfo() }
        composeButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
        
        statusLabel.text = "Initializing Transcend..."
    }

    private fun configureTranscend() {
        val config = createConfig(isStaging = true)
        
        // Init API instance
        TranscendAPI.init(applicationContext, config) { success, errorDetails ->
            // Run UI updates on the main thread since callback is invoked from background thread
            runOnUiThread {
                if (success) {
                    println("Transcend Ready!!!!!!!")
                    isTranscendReady = true
                    statusLabel.text = "✓ Transcend Ready"
                    statusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                    
                    // Enable all buttons
                    transcendButton.isEnabled = true
                    resetButton.isEnabled = true
                    printConsentButton.isEnabled = true
                    
                    // Automatically load consent info on launch
                    printConsentInfo()
                    TranscendAPI.getSdkConsentStatus(this, "com.google.android.gms:play-services-ads",
                        { consentStatus ->
                        if (consentStatus == ConsentStatus.ALLOW || consentStatus == ConsentStatus.TCF_ALLOW) {
                            Log.d("consent_gate", "firebase allowed")
                            CoroutineScope(Dispatchers.IO).launch {
                                // Initialize your non-essential SDK on a background thread.
                                // MobileAds.initialize(this@MainActivity) {}
                            }
                            val imageView =
                                findViewById<android.widget.ImageView>(R.id.imageView)
                            imageView.visibility = View.VISIBLE
                        }
                    })
                } else {
                    println("OnViewReady failed with the following error: $errorDetails")
                    statusLabel.text = "Failed to initialize Transcend"
                    statusLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                }
            }
        }
    }

    private fun resetTranscend() {
        if (!isTranscendReady) return
        
        consentTextView.text = "Resetting Transcend..."
        consentTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        
        try {
            TranscendAPI.reset { success, errorDetails ->
                if (success) {
                    consentTextView.text = "Transcend has been reset."
                    consentTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                    printConsentInfo()
                } else {
                    consentTextView.text = "Failed to reset Transcend: $errorDetails"
                    consentTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                }
            }
        } catch (e: Exception) {
            consentTextView.text = "Error resetting Transcend: ${e.message}"
            consentTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }

    private fun printConsentInfo() {
        if (!isTranscendReady) return
        
        consentTextView.text = "Loading consent info..."
        consentTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        
        fetchAndDisplayConsentInfo()
    }

    private fun fetchAndDisplayConsentInfo() {
        var consentText = "========== TRANSCEND CONSENT INFO ==========\n\n"
        var loadedCount = 0
        val totalCalls = 3
        
        fun checkCompletion() {
            loadedCount++
            if (loadedCount == totalCalls) {
                consentText += "\n============================================"
                runOnUiThread {
                    consentTextView.text = consentText
                }
            }
        }
        
        // Get Consent Details
        try {
            TranscendAPI.getConsent(applicationContext) { consentDetails ->
                consentText += "📋 CONSENT DETAILS:\n"
                consentText += "  - Confirmed: ${consentDetails.isConfirmed}\n"
                consentText += "  - Prompted: ${consentDetails.isPrompted}\n"
                consentText += "  - Timestamp: ${consentDetails.timestamp}\n"
                consentText += "  - Updated: ${consentDetails.isUpdated}\n"
                if (consentDetails.metadataTimestamp != null) {
                    consentText += "  - Metadata Timestamp: ${consentDetails.metadataTimestamp}\n"
                }
                consentText += "\n  📝 PURPOSES:\n"
                
                val purposes = consentDetails.purposes
                if (purposes != null) {
                    val sortedPurposes = purposes.toSortedMap()
                    for ((key, value) in sortedPurposes) {
                        when (value) {
                            is Boolean -> consentText += "    • $key: $value\n"
                            is String -> consentText += "    • $key: $value\n"
                            else -> consentText += "    • $key: $value\n"
                        }
                    }
                } else {
                    consentText += "    - No purposes available\n"
                }
                consentText += "\n"
                checkCompletion()
            }
        } catch (e: Exception) {
            consentText += "❌ Error getting consent details: ${e.message}\n\n"
            checkCompletion()
        }
        
        // Get Regimes
        try {
            TranscendAPI.getRegimes(applicationContext) { regimes ->
                consentText += "🌍 REGIMES:\n"
                if (regimes.isEmpty()) {
                    consentText += "  - No regimes set\n"
                } else {
                    for (regime in regimes.sorted()) {
                        consentText += "  • $regime\n"
                    }
                }
                consentText += "\n"
                checkCompletion()
            }
        } catch (e: Exception) {
            consentText += "❌ Error getting regimes: ${e.message}\n\n"
            checkCompletion()
        }
        
        // Get Regime Purposes
        try {
            TranscendAPI.getRegimePurposes(applicationContext) { regimePurposes ->
                consentText += "🎯 REGIME PURPOSES:\n"
                if (regimePurposes.isEmpty()) {
                    consentText += "  - No regime purposes set\n"
                } else {
                    for (purpose in regimePurposes.sorted()) {
                        consentText += "  • $purpose\n"
                    }
                }
                checkCompletion()
            }
        } catch (e: Exception) {
            consentText += "❌ Error getting regime purposes: ${e.message}\n"
            checkCompletion()
        }
    }

    private fun presentTranscend() {
        if (!isTranscendReady) return
        
        // Dismiss any existing bottom sheet
        bottomSheetDialog?.dismiss()
        
        val config = createConfig(isStaging = true)
        
        // Create bottom sheet dialog
        bottomSheetDialog = BottomSheetDialog(this).apply {
            val bottomSheetView = LayoutInflater.from(this@MainActivity)
                .inflate(R.layout.bottom_sheet_transcend, null)
            
            val container = bottomSheetView.findViewById<android.widget.FrameLayout>(R.id.transcendWebViewContainer)
            // Create a dedicated TranscendWebView instance for the bottom sheet
            // This prevents freezing by initializing the WebView immediately
            val webView = TranscendWebView(this@MainActivity).apply {
                // Set config first
                setConfig(config)
                // Load URL immediately to initialize the WebView (prevents freezing)
                loadUrl()
                
                // Set close listener - dismiss bottom sheet when consent manager closes
                setOnCloseListener { success, errorDetails, consentDetails ->
                    if (success) {
                        println("Transcend closed successfully")
                    } else {
                        println("Transcend closed with error: $errorDetails")
                    }
                    dismiss()
                }
            }
            
            // Add WebView to container
            container.addView(
                webView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            
            setContentView(bottomSheetView)
            
            // Set behavior to expand to full height and show consent manager
            setOnShowListener {
                val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.let {
                    val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)
                    behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                }
                // Show consent manager after dialog is shown
                webView.showConsentManager(null)
            }

            // Clean up WebView on dismiss
            setOnDismissListener {
                container.removeAllViews()
                webView.destroy()
            }
        }

        bottomSheetDialog?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Dismiss bottom sheet if open
        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
        
        // Cleanup when activity is destroyed
        if (isTranscendReady) {
            try {
                TranscendAPI.destroy()
            } catch (e: Exception) {
                println("Error destroying TranscendAPI: ${e.message}")
            }
        }
    }
}
