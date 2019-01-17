package webauthnkit.core.authenticator.internal.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ImplicitReflectionSerializer

import webauthnkit.core.ErrorReason
import webauthnkit.core.PublicKeyCredentialDescriptor
import webauthnkit.core.AuthenticatorAttachment
import webauthnkit.core.AuthenticatorTransport
import webauthnkit.core.authenticator.AuthenticatorAssertionResult
import webauthnkit.core.authenticator.AuthenticatorData
import webauthnkit.core.authenticator.GetAssertionSession
import webauthnkit.core.authenticator.GetAssertionSessionListener
import webauthnkit.core.authenticator.internal.CredentialStore
import webauthnkit.core.authenticator.internal.InternalAuthenticatorSetting
import webauthnkit.core.authenticator.internal.KeySupportChooser
import webauthnkit.core.authenticator.internal.PublicKeyCredentialSource
import webauthnkit.core.authenticator.internal.ui.UserConsentUI
import webauthnkit.core.util.AuthndroidLogger
import webauthnkit.core.util.ByteArrayUtil

@ExperimentalCoroutinesApi
@ImplicitReflectionSerializer
@ExperimentalUnsignedTypes
class InternalGetAssertionSession(
    private val setting:           InternalAuthenticatorSetting,
    private val ui:                UserConsentUI,
    private val credentialStore:   CredentialStore,
    private val keySupportChooser: KeySupportChooser
) : GetAssertionSession {

    companion object {
        val TAG = this::class.simpleName
    }

    private var started = false
    private var stopped = false

    override var listener: GetAssertionSessionListener? = null

    override val attachment: AuthenticatorAttachment
        get() = setting.attachment

    override val transport: AuthenticatorTransport
        get() = setting.transport


    override fun getAssertion(
        rpId:                          String,
        hash:                          UByteArray,
        allowCredentialDescriptorList: List<PublicKeyCredentialDescriptor>,
        requireUserPresence:           Boolean,
        requireUserVerification:       Boolean
    ) {
        AuthndroidLogger.d(TAG, "getAssertion")

        GlobalScope.launch {

            val sources = gatherCredentialSources(rpId, allowCredentialDescriptorList)

            if (sources.isEmpty()) {
                AuthndroidLogger.d(TAG, "allowable credential source not found, stop session")
                stop(ErrorReason.NotAllowed)
                return@launch
            }

            val cred = try {
                ui.requestUserSelection(
                    sources                 = sources,
                    requireUserVerification = requireUserVerification
                )
            } catch (e: Exception) {
                // TODO classify error
                stop(ErrorReason.Cancelled)
                return@launch
            }


            cred.signCount = cred.signCount + setting.counterStep

            if (!credentialStore.saveCredentialSource(cred)) {
                stop(ErrorReason.Unknown)
                return@launch
            }

            val extensions = HashMap<String, Any>()

            val rpIdHash = ByteArrayUtil.sha256(rpId)

            val authenticatorData = AuthenticatorData(
                rpIdHash               = rpIdHash.toUByteArray(),
                userPresent            = requireUserPresence,
                userVerified           = requireUserVerification,
                signCount              = cred.signCount.toUInt(),
                attestedCredentialData = null,
                extensions             = extensions
            )

            val keySupport = keySupportChooser.choose(listOf(cred.alg))
            if (keySupport == null) {
                stop(ErrorReason.Unsupported)
                return@launch
            }

            val authenticatorDataBytes = authenticatorData.toBytes()
            if (authenticatorDataBytes == null) {
                stop(ErrorReason.Unknown)
                return@launch
            }
            val dataToBeSigned = ByteArrayUtil.merge(authenticatorDataBytes, hash)

            val signature = keySupport.sign(cred.keyLabel, dataToBeSigned.toByteArray())
            if (signature == null) {
                stop(ErrorReason.Unknown)
                return@launch
            }

            val assertion =
                AuthenticatorAssertionResult(
                    credentialId      = if (allowCredentialDescriptorList.size == 1) { cred.id.toUByteArray() } else { null },
                    authenticatorData = authenticatorDataBytes,
                    signature         = signature.toUByteArray(),
                    userHandle        = cred.userHandle.toUByteArray()
                )


            onComplete()

            listener?.onCredentialDiscovered(this@InternalGetAssertionSession, assertion)
        }
    }

    override fun canPerformUserVerification(): Boolean {
        AuthndroidLogger.d(TAG, "canPerformUserVerification")
        return this.setting.allowUserVerification
    }

    override fun start() {
        AuthndroidLogger.d(TAG, "start")
        if (stopped) {
            AuthndroidLogger.d(TAG, "already stopped")
            return
        }
        if (started) {
            AuthndroidLogger.d(TAG, "already started")
            return
        }
        started = true
        listener?.onAvailable(this)
    }

    override fun cancel(reason: ErrorReason) {
        AuthndroidLogger.d(TAG, "cancel")
        if (stopped) {
            AuthndroidLogger.d(TAG, "already stopped")
            return
        }
        /* TODO cancel UI
        if (ui.opened) {

            return
        }
        */
        stop(reason)
    }

    private fun stop(reason: ErrorReason) {
        AuthndroidLogger.d(TAG, "stop")
        if (!started) {
            AuthndroidLogger.d(TAG, "not started")
            return
        }
        if (stopped) {
            AuthndroidLogger.d(TAG, "already stopped")
            return
        }
        stopped = true
        listener?.onOperationStopped(this, reason)
    }

    private fun onComplete() {
        AuthndroidLogger.d(TAG, "onComplete")
        stopped = true
    }

    private fun gatherCredentialSources(
        rpId: String,
        allowCredentialDescriptorList: List<PublicKeyCredentialDescriptor>
    ): List<PublicKeyCredentialSource> {

        return if (allowCredentialDescriptorList.isEmpty()) {

            credentialStore.loadAllCredentialSources(rpId)

        } else {

            allowCredentialDescriptorList.mapNotNull {
                credentialStore.lookupCredentialSource(it.id.toByteArray())
            }

        }
    }

}