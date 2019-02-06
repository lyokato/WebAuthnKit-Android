package webauthnkit.core.authenticator.internal.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import webauthnkit.core.authenticator.AuthenticatorAssertionResult
import webauthnkit.core.authenticator.AuthenticatorData
import webauthnkit.core.authenticator.GetAssertionSession
import webauthnkit.core.authenticator.GetAssertionSessionListener
import webauthnkit.core.authenticator.internal.CredentialStore
import webauthnkit.core.authenticator.internal.InternalAuthenticatorSetting
import webauthnkit.core.authenticator.internal.key.KeySupportChooser
import webauthnkit.core.authenticator.internal.PublicKeyCredentialSource
import webauthnkit.core.authenticator.internal.ui.UserConsentUI
import webauthnkit.core.error.*
import webauthnkit.core.data.*
import webauthnkit.core.error.TimeoutException
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.util.ByteArrayUtil

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class InternalGetAssertionSession(
    private val setting:           InternalAuthenticatorSetting,
    private val ui:                UserConsentUI,
    private val credentialStore:   CredentialStore,
    private val keySupportChooser: KeySupportChooser
) : GetAssertionSession {

    companion object {
        val TAG = InternalGetAssertionSession::class.simpleName
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
        hash:                          ByteArray,
        allowCredentialDescriptorList: List<PublicKeyCredentialDescriptor>,
        requireUserPresence:           Boolean,
        requireUserVerification:       Boolean
    ) {
        WAKLogger.d(TAG, "getAssertion")

        GlobalScope.launch {

            val sources =
                gatherCredentialSources(rpId, allowCredentialDescriptorList)

            if (sources.isEmpty()) {
                WAKLogger.d(TAG, "allowable credential source not found, stop session")
                stop(ErrorReason.NotAllowed)
                return@launch
            }

            val cred = try {
                WAKLogger.d(TAG, "request user selection")
                ui.requestUserSelection(
                    sources                 = sources,
                    requireUserVerification = requireUserVerification
                )
            } catch (e: CancelledException) {
                WAKLogger.d(TAG, "failed to select $e")
                stop(ErrorReason.Cancelled)
                return@launch
            } catch (e: TimeoutException) {
                WAKLogger.d(TAG, "failed to select $e")
                stop(ErrorReason.Timeout)
                return@launch
            } catch (e: Exception) {
                WAKLogger.d(TAG, "failed to select $e")
                stop(ErrorReason.Unknown)
                return@launch
            }


            cred.signCount = cred.signCount + setting.counterStep

            WAKLogger.d(TAG, "update credential")

            if (!credentialStore.saveCredentialSource(cred)) {

                WAKLogger.d(TAG, "failed to update credential")
                stop(ErrorReason.Unknown)
                return@launch

            }

            val extensions = HashMap<String, Any>()

            val rpIdHash = ByteArrayUtil.sha256(rpId)

            val authenticatorData = AuthenticatorData(
                rpIdHash               = rpIdHash,
                userPresent            = (requireUserPresence || requireUserVerification),
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

            val dataToBeSigned =
                ByteArrayUtil.merge(authenticatorDataBytes, hash)

            val signature = keySupport.sign(cred.keyLabel, dataToBeSigned)
            if (signature == null) {
                stop(ErrorReason.Unknown)
                return@launch
            }

            val credentialId =
                if (allowCredentialDescriptorList.size != 1) { cred.id } else { null }

            val assertion =
                AuthenticatorAssertionResult(
                    credentialId      = credentialId,
                    authenticatorData = authenticatorDataBytes,
                    signature         = signature,
                    userHandle        = cred.userHandle
                )


            onComplete()

            listener?.onCredentialDiscovered(this@InternalGetAssertionSession, assertion)
        }
    }

    override fun canPerformUserVerification(): Boolean {
        WAKLogger.d(TAG, "canPerformUserVerification")
        return this.setting.allowUserVerification
    }

    override fun start() {
        WAKLogger.d(TAG, "start")
        if (stopped) {
            WAKLogger.d(TAG, "already stopped")
            return
        }
        if (started) {
            WAKLogger.d(TAG, "already started")
            return
        }
        started = true
        listener?.onAvailable(this)
    }

    override fun cancel(reason: ErrorReason) {
        WAKLogger.d(TAG, "cancel")
        if (stopped) {
            WAKLogger.d(TAG, "already stopped")
            return
        }
        if (ui.isOpen) {
            ui.cancel(reason)
            return
        }
        stop(reason)
    }

    private fun stop(reason: ErrorReason) {
        WAKLogger.d(TAG, "stop")
        if (!started) {
            WAKLogger.d(TAG, "not started")
            return
        }
        if (stopped) {
            WAKLogger.d(TAG, "already stopped")
            return
        }
        stopped = true
        listener?.onOperationStopped(this, reason)
    }

    private fun onComplete() {
        WAKLogger.d(TAG, "onComplete")
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
                credentialStore.lookupCredentialSource(it.id)
            }

        }
    }

}