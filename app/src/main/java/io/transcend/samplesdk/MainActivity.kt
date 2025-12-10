package io.transcend.samplesdk

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import io.transcend.webview.IABConstants
import io.transcend.webview.TranscendAPI
import io.transcend.webview.TranscendConstants
import io.transcend.webview.TranscendWebView
import io.transcend.webview.models.TrackingConsentDetails
import io.transcend.webview.models.TranscendConfig
import io.transcend.webview.models.TranscendCoreConfig
import java.util.HashMap

// MainActivity is the login page
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpTranscendWebView()
        setUpButtons()
    }

    private fun setUpButtons() {
        val button = findViewById<Button>(R.id.Home)
        button.setOnClickListener {
            // Create an Intent to start the HomeActivity
            val intent = Intent(this@MainActivity, HomeActivity::class.java)
            startActivity(intent)
        }

        val manageConsentButton = findViewById<Button>(R.id.manageConsentPreferences)
        manageConsentButton.setOnClickListener {
            // Create an Intent to start the ManageConsentPreferences
            val intent = Intent(this@MainActivity, ManageConsentPreferences::class.java)
            startActivity(intent)
        }
    }

    private fun setUpTranscendWebView() {
        val url = "https://transcend-cdn.com/cm-test/e622c065-89e6-4b3c-b31f-46428a1e7b86/airgap.js"

        // Specify any default airgap attributes
        val agAttributes = HashMap<String, String>().apply {
            put("data-regime", "GDPR")
        }

        // Create config Object
        val config = TranscendConfig.ConfigBuilder(url)
            .defaultAttributes(agAttributes)
            .destroyOnClose(false)
            .autoShowUI(false)
            .mobileAppId("RideshareTest")
            .build()

        val layout = findViewById<LinearLayout>(R.id.contentView)
        val transcendWebView = findViewById<TranscendWebView>(R.id.transcendWebView)
        // Set config for element defined on layout
        transcendWebView.setConfig(config)
        transcendWebView.setOnCloseListener { success, errorDetails, consentDetails ->
            if (success) {
                println("In onCloseListener::${consentDetails.isConfirmed}")
                println("User Purposes::${consentDetails.purposes}")
                getSdkConsentStatus()
                layout.visibility = View.VISIBLE
            } else {
                println("OnCloseListener failed with the following error: $errorDetails")
                layout.visibility = View.VISIBLE
            }
        }
        transcendWebView.loadUrl()

        // Init API instance by passing config
        TranscendAPI.init(applicationContext, config) { success, errorDetails ->
            if (success) {
                try {
                    println("Transcend Ready!!!!!!!")
                    TranscendAPI.getConsent(applicationContext) { trackingConsentDetails ->
                        println("isConfirmed: ${trackingConsentDetails.isConfirmed}")
                        println("SharedPreferences: ${PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(TranscendConstants.TRANSCEND_CONSENT_DATA, "lol")}")
                        println("GDPR_APPLIES from SharedPreferences: ${PreferenceManager.getDefaultSharedPreferences(applicationContext).getInt(IABConstants.IAB_TCF_GDPR_APPLIES, 100)}")
                        fetchRegimesAndHandleUI(transcendWebView, config, trackingConsentDetails)
                    }
                    TranscendAPI.getLoadOptions(applicationContext) { loadOptions ->
                        println("loadOptions: $loadOptions")
                    }
                    TranscendAPI.getRegimePurposes(applicationContext) { regimePurposes ->
                        println("regimePurposes: $regimePurposes")
                    }
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            } else {
                println("OnViewReady failed with the following error: $errorDetails")
                layout.visibility = View.VISIBLE
            }
        }
    }

    private fun fetchRegimesAndHandleUI(
        transcendWebView: TranscendWebView,
        config: TranscendCoreConfig,
        trackingConsentDetails: TrackingConsentDetails
    ) {
        try {
            TranscendAPI.getRegimes(applicationContext) { regimes ->
                println("regimes: ${regimes.toString()}")
                if (true) {
                    println("Requesting user consent...")
                    transcendWebView.visibility = View.VISIBLE
                } else {
                    transcendWebView.hideConsentManager()
                    val contentView = findViewById<LinearLayout>(R.id.contentView)
                    contentView.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            println("Found error on getRegimes()")
        }
    }

    private fun getSdkConsentStatus() {
        try {
            TranscendAPI.getSdkConsentStatus(applicationContext, "appsflyer-android") { consentStatus ->
                println(consentStatus.toString())
            }
        } catch (e: Exception) {
            println("Found error on getServiceConsentStatus()")
        }
    }
}
