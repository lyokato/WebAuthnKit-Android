package webauthnkit.core.authenticator.internal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ImplicitReflectionSerializer

import webauthnkit.core.AuthenticatorAttachment
import webauthnkit.core.AuthenticatorTransport
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.authenticator.Authenticator
import webauthnkit.core.authenticator.GetAssertionSession
import webauthnkit.core.authenticator.MakeCredentialSession
import webauthnkit.core.authenticator.internal.session.InternalGetAssertionSession
import webauthnkit.core.authenticator.internal.session.InternalMakeCredentialSession
import webauthnkit.core.authenticator.internal.ui.UserConsentUI

@ExperimentalUnsignedTypes
class InternalAuthenticatorSetting {
    val attachment = AuthenticatorAttachment.Platform
    val transport  = AuthenticatorTransport.Internal
    var counterStep: UInt = 1u
    var allowUserVerification = true
}

@ExperimentalCoroutinesApi
@ImplicitReflectionSerializer
@ExperimentalUnsignedTypes
class InternalAuthenticator(
    private val ui:                UserConsentUI,
    private val credentialStore:   CredentialStore,
    private val keySupportChooser: KeySupportChooser
) : Authenticator {

    companion object {
        val TAG = this::class.simpleName
    }

    private val setting = InternalAuthenticatorSetting()

    override val attachment: AuthenticatorAttachment
        get() = setting.attachment

    override val transport: AuthenticatorTransport
        get() = setting.transport

    override var counterStep: UInt
        get() = setting.counterStep
        set(value) { setting.counterStep = value }

    override val allowResidentKey: Boolean = true

    override var allowUserVerification: Boolean
        get() = setting.allowUserVerification
        set(value) { setting.allowUserVerification = value }

    override fun newGetAssertionSession(): GetAssertionSession {
        WAKLogger.d(TAG, "newGetAssertionSession")
        return InternalGetAssertionSession(
            setting           = setting,
            ui                = ui,
            credentialStore   = credentialStore,
            keySupportChooser = keySupportChooser
        )
    }

    override fun newMakeCredentialSession(): MakeCredentialSession {
        WAKLogger.d(TAG, "newMakeCredentialSession")
        return InternalMakeCredentialSession(
            setting           = setting,
            ui                = ui,
            credentialStore   = credentialStore,
            keySupportChooser = keySupportChooser
        )
    }


}