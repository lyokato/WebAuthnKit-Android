package webauthnkit.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity;
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ImplicitReflectionSerializer
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import webauthnkit.core.AuthenticatorTransport
import webauthnkit.core.GetAssertionResponse
import webauthnkit.core.PublicKeyCredentialRequestOptions

import webauthnkit.core.UserVerificationRequirement
import webauthnkit.core.authenticator.internal.CredentialStore
import webauthnkit.core.authenticator.internal.InternalAuthenticator
import webauthnkit.core.authenticator.internal.KeySupportChooser
import webauthnkit.core.authenticator.internal.ui.UserConsentUI
import webauthnkit.core.client.WebAuthnClient
import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.WAKLogger

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        private val TAG = this::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userVerificationOptions = listOf("Required", "Preferred", "Discouraged")

        verticalLayout {

            textView {
                text = "Relying Party"
            }

            val relyingPartyField = editText {
                singleLine = true
            }
            relyingPartyField.setText("https://example.org")

            textView {
                text = "Challenge (Hex)"
            }

            val challengeField = editText {
                singleLine = true
            }
            challengeField.setText("aed9c789543b")

            textView {
                text = "Credential Id (Hex) (Optional)"
            }

            val credIdField = editText {
                singleLine = true
            }
            credIdField.setText("")

            val spinnerWidth = 160
            var userVerificationSpinner: MaterialSpinner? = null

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

            button("Authenticate") {
                onClick {

                    val relyingParty= relyingPartyField.text.toString()
                    val credId      = credIdField.text.toString()
                    val challenge   = challengeField.text.toString()

                    val userVerification  =
                        when (userVerificationOptions[userVerificationSpinner!!.selectedIndex]) {
                            "Required"    -> { UserVerificationRequirement.Required    }
                            "Preferred"   -> { UserVerificationRequirement.Preferred   }
                            "Discouraged" -> { UserVerificationRequirement.Discouraged }
                            else          -> { UserVerificationRequirement.Preferred   }
                        }

                    onExecute(
                        relyingParty     = relyingParty,
                        challenge        = challenge,
                        credId           = credId,
                        userVerification = userVerification
                    )
                }
            }
        }

    }

    var client: WebAuthnClient? = null

    private fun onExecute(relyingParty: String, challenge: String,
                          credId: String, userVerification: UserVerificationRequirement) {

        val options = PublicKeyCredentialRequestOptions()
        options.challenge        = ByteArrayUtil.fromHex(challenge)
        options.rpId             = relyingParty
        options.userVerification = userVerification

        if (credId.isNotEmpty()) {
            options.addAllowCredential(
                credentialId = ByteArrayUtil.fromHex(credId),
                transports   = mutableListOf(AuthenticatorTransport.Internal))
        }

        client = createClient()
        val operation = client!!.get(options)

        GlobalScope.launch {
            val cred = operation.start()
            WAKLogger.d(TAG, "CHALLENGE:" + ByteArrayUtil.encodeBase64URL(options.challenge))
            showResultActivity(cred)
        }

    }

    private fun showResultActivity(cred: GetAssertionResponse) {
        runOnUiThread {
            val intent = Intent(this, AuthenticationResultActivity::class.java)
            intent.putExtra("CRED_ID", cred.id)
            intent.putExtra("CRED_RAW", ByteArrayUtil.toHex(cred.rawId))
            intent.putExtra("CLIENT_JSON", cred.response.clientDataJSON)
            intent.putExtra("AUTHENTICATOR_DATA", ByteArrayUtil.encodeBase64URL(cred.response.authenticatorData))
            intent.putExtra("SIGNATURE", ByteArrayUtil.toHex(cred.response.signature))
            intent.putExtra("USER_HANDLE", String(bytes = cred.response.userHandle!!, charset = Charsets.UTF_8))
            startActivity(intent)
        }
    }

    private fun createClient(): WebAuthnClient {

        val ui = UserConsentUI(this)

        val authenticator = InternalAuthenticator(
            ui                = ui,
            credentialStore   = CredentialStore(this),
            keySupportChooser = KeySupportChooser(this)
        )

        return WebAuthnClient(
            origin        = "https://example.org",
            authenticator = authenticator
        )

    }

}
