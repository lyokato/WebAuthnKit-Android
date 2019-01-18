package webauthnkit.example

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ImplicitReflectionSerializer
import org.jetbrains.anko.*
import webauthnkit.core.authenticator.internal.CredentialStore
import webauthnkit.core.authenticator.internal.InternalAuthenticator
import webauthnkit.core.authenticator.internal.KeySupportChooser
import webauthnkit.core.authenticator.internal.ui.UserConsentUI

import webauthnkit.core.client.WebAuthnClient

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
class RegistrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val userVerificationOptions = listOf("Required", "Preferred", "Discouraged")
        val attestationConveyanceOptions = listOf("Direct", "Indirect", "None")

        verticalLayout {

            padding = dip(10)

            textView {
                text = "User Id"
            }

            val userId = editText {
                singleLine = true
            }
            userId.setText("lyokato")

            textView {
                text = "User Name"
            }

            val userName = editText {
                singleLine = true
            }
            userName.setText("lyokato")

            textView {
                text = "User Display Name"
            }

            val userDisplayName = editText {
                singleLine = true
            }
            userDisplayName.setText("Lyo Kato")

            textView {
                text = "User ICON URL (Optional)"
            }

            val userIconURL = editText {
                singleLine = true
            }
            userIconURL.setText("https://www.gravatar.com/avatar/0b63462eb18efbfb764b0c226abff4a0?s=440&d=retro")

            textView {
                text = "Relying Party"
            }

            val relyingParty = editText {
                singleLine = true
            }
            relyingParty.setText("https://example.org")

            textView {
                text = "Relying Party ICON"
            }

            val relyingPartyICON = editText {
                singleLine = true
            }
            relyingPartyICON.setText("https://developers.google.com/identity/images/g-logo.png")

            textView {
                text = "Challenge"
            }

            val challenge = editText {
                singleLine = true
            }
            challenge.setText("aed9c789543b")

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

                val userVerification = materialSpinner {

                    padding = dip(10)

                    lparams {
                        width = dip(spinnerWidth)
                        height = wrapContent
                        margin = dip(10)
                        alignParentRight()
                        centerVertically()
                    }
                }

                userVerification.setItems(userVerificationOptions)
            }

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

                val attestationConveyance = materialSpinner {

                    padding = dip(10)

                    lparams {
                        width = dip(spinnerWidth)
                        height = wrapContent
                        margin = dip(10)
                        alignParentRight()
                        centerVertically()
                    }
                }

                attestationConveyance.setItems(attestationConveyanceOptions)

            }


            button("Register") {

                onExecute()
            }
        }
    }

    private fun onExecute() {

    }

    private fun createClient(): WebAuthnClient {

        val ui = UserConsentUI(this)

        val authenticator = InternalAuthenticator(
            ui                = ui,
            credentialStore   = CredentialStore(this),
            keySupportChooser = KeySupportChooser(this)
        )

        val client = WebAuthnClient(
            origin        = "https://example.org/",
            authenticator = authenticator
        )

        return client
    }

}
