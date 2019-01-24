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
import webauthnkit.core.*
import webauthnkit.core.authenticator.COSEAlgorithmIdentifier
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
class RegistrationActivity : AppCompatActivity() {

    companion object {
        private val TAG = RegistrationActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val userVerificationOptions = listOf("Required", "Preferred", "Discouraged")
        val attestationConveyanceOptions = listOf("Direct", "Indirect", "None")

        verticalLayout {

            padding = dip(10)

            textView {
                text = "User Id"
            }

            val userIdField = editText {
                singleLine = true
            }
            userIdField.setText("lyokato")

            textView {
                text = "User Name"
            }

            val userNameField = editText {
                singleLine = true
            }
            userNameField.setText("lyokato")

            textView {
                text = "User Display Name"
            }

            val userDisplayNameField = editText {
                singleLine = true
            }
            userDisplayNameField.setText("Lyo Kato")

            textView {
                text = "User ICON URL (Optional)"
            }

            val userIconURLField = editText {
                singleLine = true
            }
            userIconURLField.setText("https://www.gravatar.com/avatar/0b63462eb18efbfb764b0c226abff4a0?s=440&d=retro")

            textView {
                text = "Relying Party"
            }

            val relyingPartyField = editText {
                singleLine = true
            }
            relyingPartyField.setText("https://example.org")

            textView {
                text = "Relying Party ICON"
            }

            val relyingPartyICONField = editText {
                singleLine = true
            }
            relyingPartyICONField.setText("https://developers.google.com/identity/images/g-logo.png")

            textView {
                text = "Challenge (Hex)"
            }

            val challengeField = editText {
                singleLine = true
            }
            challengeField.setText("aed9c789543b")

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

            var attestationConveyanceSpinner: MaterialSpinner? = null

            relativeLayout {

                lparams {
                    width = matchParent
                    height = wrapContent
                    margin = dip(10)
                }

                backgroundColor = Color.parseColor("#eeeeee")

                textView {
                    padding = dip(10)
                    text = "Attestation"

                }.lparams {
                    width = wrapContent
                    height = wrapContent
                    margin = dip(10)
                    alignParentLeft()
                    centerVertically()
                }

                attestationConveyanceSpinner = materialSpinner {

                    padding = dip(10)

                    lparams {
                        width = dip(spinnerWidth)
                        height = wrapContent
                        margin = dip(10)
                        alignParentRight()
                        centerVertically()
                    }
                }

                attestationConveyanceSpinner!!.setItems(attestationConveyanceOptions)
            }

            button("Register") {

                onClick {

                    // TODO validation
                    val userId          = userIdField.text.toString()
                    val username        = userNameField.text.toString()
                    val userDisplayName = userDisplayNameField.text.toString()
                    val userIconURL     = userIconURLField.text.toString()
                    val relyingParty    = relyingPartyField.text.toString()
                    val relyingPartyICON= relyingPartyICONField.text.toString()
                    val challenge       = challengeField.text.toString()

                    val userVerification  =
                        when (userVerificationOptions[userVerificationSpinner!!.selectedIndex]) {
                            "Required"    -> { UserVerificationRequirement.Required    }
                            "Preferred"   -> { UserVerificationRequirement.Preferred   }
                            "Discouraged" -> { UserVerificationRequirement.Discouraged }
                            else          -> { UserVerificationRequirement.Preferred   }
                        }
                    val attestationConveyance =
                        when (attestationConveyanceOptions[attestationConveyanceSpinner!!.selectedIndex]) {
                            "Direct"   -> { AttestationConveyancePreference.Direct   }
                            "Indirect" -> { AttestationConveyancePreference.Indirect }
                            "None"     -> { AttestationConveyancePreference.None     }
                            else       -> { AttestationConveyancePreference.Direct   }
                        }


                    onExecute(
                        userId                = userId,
                        username              = username,
                        userDisplayName       = userDisplayName,
                        userIconURL           = userIconURL,
                        relyingParty          = relyingParty,
                        relyingPartyICON      = relyingPartyICON,
                        challenge             = challenge,
                        userVerification      = userVerification,
                        attestationConveyance = attestationConveyance
                    )

                }

            }
        }
    }

    var client: WebAuthnClient? = null

    private fun onExecute(userId: String, username: String, userDisplayName: String,
                          userIconURL: String, relyingParty: String, relyingPartyICON: String,
                          challenge: String, userVerification: UserVerificationRequirement,
                          attestationConveyance: AttestationConveyancePreference) {

        val options = PublicKeyCredentialCreationOptions()
        options.challenge = ByteArrayUtil.fromHex(challenge)
        options.user.id = userId
        options.user.name = username
        options.user.displayName = userDisplayName
        options.user.icon = userIconURL
        options.rp.id = relyingParty
        options.rp.name = relyingParty
        options.rp.icon = relyingPartyICON
        options.attestation = attestationConveyance
        options.addPubKeyCredParam(alg = COSEAlgorithmIdentifier.es256)
        options.authenticatorSelection = AuthenticatorSelectionCriteria(
            requireResidentKey = true,
            userVerification   = userVerification
        )

        client = createClient()
        val operation = client!!.create(options)

        GlobalScope.launch {
            try {

                val cred = operation.start()
                WAKLogger.d(TAG, "CHALLENGE:" + ByteArrayUtil.encodeBase64URL(options.challenge))

                runOnUiThread {
                    showResultActivity(cred)
                }

            } catch (e: Exception) {

                WAKLogger.w(TAG, "failed to create")

                runOnUiThread {
                    toast(e.toString())
                }

            }
        }
    }

    private fun showResultActivity(cred: MakeCredentialResponse) {
        runOnUiThread {
            val intent = Intent(this, RegistrationResultActivity::class.java)
            intent.putExtra("CRED_ID", cred.id)
            intent.putExtra("CRED_RAW", ByteArrayUtil.toHex(cred.rawId))
            intent.putExtra("ATTESTATION", ByteArrayUtil.encodeBase64URL(cred.response.attestationObject))
            intent.putExtra("CLIENT_JSON", cred.response.clientDataJSON)
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
