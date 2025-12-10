package io.transcend.samplesdk

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import io.transcend.webview.IABConstants
import io.transcend.webview.TranscendAPI
import io.transcend.webview.TranscendConstants
import io.transcend.webview.TranscendWebView
import io.transcend.webview.models.TrackingConsentDetails

class ManageConsentPreferences : AppCompatActivity() {
    private var context: Context? = null
    private lateinit var transcendWebView: TranscendWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_consent_preferences)

        println("ManageConsentPreferences - onCreate")
        // Can either create a clone of previous defined config
        // if you need any changes
        // Example: here I want to change the destroy on close behavior to false
        val config = TranscendAPI.config.clone()
        config.setDestroyOnClose(false)
        // Or could also directly use same config object created on MainActivity as follows
        // if no change needed
        // config = TranscendAPI.config
        transcendWebView = findViewById(R.id.transcendWebView)
        transcendWebView.setConfig(config)
        transcendWebView.loadUrl()
        setUpButtons()
        context = this
    }

    private fun setUpButtons() {
        val button = findViewById<Button>(R.id.changeConsent)
        button.setOnClickListener {
            transcendWebView.visibility = View.VISIBLE
            transcendWebView.showConsentManager(null)
        }

        val manageConsentButton = findViewById<Button>(R.id.logConsent)
        manageConsentButton.setOnClickListener {
            getAndLogConsent()
        }
    }

    private fun getAndLogConsent() {
        try {
            TranscendAPI.getConsent(applicationContext) { consentDetails ->
                println("getConsent().isConfirmed(): ${consentDetails.isConfirmed}")
                println("getConsent().getPurposes():${consentDetails.purposes}")
                println("SharedPreferences: ${PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(TranscendConstants.TRANSCEND_CONSENT_DATA, "null")}")
                println("GDPR_APPLIES from SharedPreferences: ${PreferenceManager.getDefaultSharedPreferences(applicationContext).getInt(IABConstants.IAB_TCF_GDPR_APPLIES, -1)}")
            }
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }
}

