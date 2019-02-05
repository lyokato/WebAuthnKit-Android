package webauthnkit.core.authenticator.internal

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi

import webauthnkit.core.data.*
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.authenticator.Authenticator
import webauthnkit.core.authenticator.GetAssertionSession
import webauthnkit.core.authenticator.MakeCredentialSession
import webauthnkit.core.authenticator.internal.key.KeySupportChooser
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
@ExperimentalUnsignedTypes
class InternalAuthenticator(
    private val activity:          FragmentActivity,
    private val ui:                UserConsentUI,
    private val credentialStore:   CredentialStore = CredentialStore(activity),
    private val keySupportChooser: KeySupportChooser = KeySupportChooser(activity)
) : Authenticator {

    companion object {
        val TAG = InternalAuthenticator::class.simpleName
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