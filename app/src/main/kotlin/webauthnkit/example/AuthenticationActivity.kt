package webauthnkit.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.*
import webauthnkit.core.data.*
import webauthnkit.core.authenticator.internal.ui.UserConsentUI
import webauthnkit.core.authenticator.internal.ui.UserConsentUIFactory
import webauthnkit.core.client.WebAuthnClient
import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.WAKLogger

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        private val TAG = AuthenticationActivity::class.simpleName
    }

    var relyingPartyField:     EditText? = null
    var challengeField:        EditText? = null
    var credIdField:           EditText? = null

    var userVerificationSpinner: MaterialSpinner? = null
    val userVerificationOptions = listOf("Required", "Preferred", "Discouraged")

    override fun onCreate(savedInstanceState: Bundle?) {
        WAKLogger.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        title = "AUTHENTICATION"

        verticalLayout {

            padding = dip(10)

            textView {
                text = "Relying Party"
            }

            relyingPartyField = editText {
                singleLine = true
            }
            relyingPartyField!!.setText("https://example.org")

            textView {
                text = "Challenge (Hex)"
            }

            challengeField = editText {
                singleLine = true
            }
            challengeField!!.setText("aed9c789543b")

            textView {
                text = "Credential Id (Hex) (Optional)"
            }

            credIdField = editText {
                singleLine = true
            }
            credIdField!!.setText("")

            val spinnerWidth = 160

            relativeLayout {

                lparams {
                    width = matchParent
                    height = wrapContent
                    margin = dip(10)
                }

                backgroundColor = Color.parseColor("#eeeeee")

                textView {
                    padding = dip(10)
                    text = "UV"

                }.lparams {
                    width = wrapContent
                    height = wrapContent
                    margin = dip(10)
                    alignParentLeft()
                    centerVertically()
                }

                userVerificationSpinner = materialSpinner {

                    padding = dip(10)

                    lparams {
                        width = dip(spinnerWidth)
                        height = wrapContent
                        margin = dip(10)
                        alignParentRight()
                        centerVertically()
                    }
                }

                userVerificationSpinner!!.setItems(userVerificationOptions)
            }
        }

    }

    private fun onStartClicked() {

        val relyingParty= relyingPartyField!!.text.toString()
        val credId      = credIdField!!.text.toString()
        val challenge   = challengeField!!.text.toString()

        val userVerification  =
            when (userVerificationOptions[userVerificationSpinner!!.selectedIndex]) {
                "Required"    -> { UserVerificationRequirement.Required    }
                "Preferred"   -> { UserVerificationRequirement.Preferred   }
                "Discouraged" -> { UserVerificationRequirement.Discouraged }
                else          -> { UserVerificationRequirement.Preferred   }
            }

        GlobalScope.launch {

            onExecute(
                relyingParty     = relyingParty,
                challenge        = challenge,
                credId           = credId,
                userVerification = userVerification
            )


        }

    }

    private fun createWebAuthnClient(): WebAuthnClient {


        consentUI = UserConsentUIFactory.create(this)
        WAKLogger.d(TAG, "create consentUI===========================================")

        return WebAuthnClient.internal(
            activity = this,
            origin   = "https://example.org",
            ui       = consentUI!!
        )
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

    override fun onStart() {
        WAKLogger.d(TAG, "onStart")
        super.onStart()
    }

    override fun onStop() {
        WAKLogger.d(TAG, "onStop")
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.authentication_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.webauthn_authentication_start) {
            onStartClicked()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    var consentUI: UserConsentUI? = null
    var webAuthnClient: WebAuthnClient? = null

    private suspend fun onExecute(relyingParty: String, challenge: String,
                          credId: String, userVerification: UserVerificationRequirement) {

        WAKLogger.d(TAG, "onExecute")
        val options = PublicKeyCredentialRequestOptions()
        options.challenge        = ByteArrayUtil.fromHex(challenge)
        options.rpId             = relyingParty
        options.userVerification = userVerification

        if (credId.isNotEmpty()) {
            options.addAllowCredential(
                credentialId = ByteArrayUtil.fromHex(credId),
                transports   = mutableListOf(AuthenticatorTransport.Internal))
        }

        webAuthnClient = createWebAuthnClient()

        try {

            val cred = webAuthnClient!!.get(options)
            WAKLogger.d(TAG, "CHALLENGE:" + ByteArrayUtil.encodeBase64URL(options.challenge))
            showResultActivity(cred)

        } catch (e: Exception) {

            WAKLogger.w(TAG, "failed to get")
            showErrorPopup(e.toString())

        } finally {
            consentUI = null
        }
    }

    private fun showErrorPopup(msg: String) {
        runOnUiThread {
            toast(msg)
        }
    }

    private fun showResultActivity(cred: GetAssertionResponse) {
        WAKLogger.d(TAG, "show result activity")
        runOnUiThread {
            val intent = Intent(this, AuthenticationResultActivity::class.java)
            intent.putExtra("CRED_ID", cred.id)
            intent.putExtra("CRED_RAW", ByteArrayUtil.toHex(cred.rawId))
            intent.putExtra("CLIENT_JSON", cred.response.clientDataJSON)
            intent.putExtra("AUTHENTICATOR_DATA", ByteArrayUtil.encodeBase64URL(cred.response.authenticatorData))
            intent.putExtra("SIGNATURE", ByteArrayUtil.encodeBase64URL(cred.response.signature))
            intent.putExtra("USER_HANDLE", ByteArrayUtil.encodeBase64URL(cred.response.userHandle!!))
            WAKLogger.d(TAG, "start activity")
            startActivity(intent)
        }
    }
}
