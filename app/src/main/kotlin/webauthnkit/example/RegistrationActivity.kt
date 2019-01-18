package webauthnkit.example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ImplicitReflectionSerializer
import org.jetbrains.anko.*
import webauthnkit.core.authenticator.Authenticator
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

        verticalLayout {
            button("Register") {

            }
        }
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
