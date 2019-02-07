package webauthnkit.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import webauthnkit.core.authenticator.internal.InternalAuthenticator
import webauthnkit.core.authenticator.internal.ui.UserConsentUI
import webauthnkit.core.authenticator.internal.ui.UserConsentUIFactory
import webauthnkit.core.ctap.ble.BleFidoService
import webauthnkit.core.ctap.ble.BleFidoServiceListener
import webauthnkit.core.util.WAKLogger

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class BleAuthenticatorRegistrationActivity : AppCompatActivity() {

    companion object {
        val TAG = BleAuthenticatorRegistrationActivity::class.simpleName
    }

    var consentUI: UserConsentUI? = null
    var bleFidoService: BleFidoService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "REGISTRATION BLE SERVICE"

        verticalLayout {

            padding = dip(10)

            button("START") {
                textSize = 24f

                onClick {

                    onStartClicked()

                }

            }

            button("STOP") {
                textSize = 24f

                onClick {

                    onStopClicked()

                }
            }

            createBleFidoService()
        }
    }

    private fun onStartClicked() {
        WAKLogger.d(TAG, "onStartClicked")
        if (bleFidoService!!.start()) {
            WAKLogger.d(TAG, "started successfully")
        } else {
            WAKLogger.d(TAG, "failed to start")
        }
    }

    private fun onStopClicked() {
        WAKLogger.d(TAG, "onStopClicked")
        bleFidoService?.stop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        WAKLogger.d(TAG, "onActivityResult")
        consentUI?.onActivityResult(requestCode, resultCode, data)
        /*
        if (consentUI != null && consentUI!!.onActivityResult(requestCode, resultCode, data)) {
            return
        }
        */
    }

    val bleServiceListener = object: BleFidoServiceListener {

        override fun onConnected(address: String) {
            WAKLogger.d(TAG, "onConnected")
        }

        override fun onDisconnected(address: String) {
            WAKLogger.d(TAG, "onDisconnected")
        }

        override fun onClosed() {
            WAKLogger.d(TAG, "onClosed")
        }
    }


    private fun createBleFidoService() {

        consentUI = UserConsentUIFactory.create(this)

        bleFidoService = BleFidoService.create(
            activity  = this,
            ui        = consentUI!!,
            listener  = bleServiceListener
        )
    }

}
