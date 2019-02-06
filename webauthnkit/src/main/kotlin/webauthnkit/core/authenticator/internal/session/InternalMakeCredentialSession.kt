package webauthnkit.core.authenticator.internal.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import webauthnkit.core.authenticator.AttestedCredentialData
import webauthnkit.core.authenticator.AuthenticatorData
import webauthnkit.core.authenticator.MakeCredentialSession
import webauthnkit.core.authenticator.MakeCredentialSessionListener
import webauthnkit.core.authenticator.internal.CredentialStore
import webauthnkit.core.authenticator.internal.InternalAuthenticatorSetting
import webauthnkit.core.authenticator.internal.key.KeySupportChooser
import webauthnkit.core.authenticator.internal.PublicKeyCredentialSource
import webauthnkit.core.authenticator.internal.ui.UserConsentUI
import webauthnkit.core.error.*
import webauthnkit.core.data.*
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.util.ByteArrayUtil
import java.util.*

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class InternalMakeCredentialSession(
    private val setting:           InternalAuthenticatorSetting,
    private val ui:                UserConsentUI,
    private val credentialStore:   CredentialStore,
    private val keySupportChooser: KeySupportChooser
) : MakeCredentialSession {

    companion object {
        val TAG = InternalMakeCredentialSession::class.simpleName
    }

    private var stopped = false
    private var started = false

    override var listener: MakeCredentialSessionListener? = null

    override val attachment: AuthenticatorAttachment
        get() = setting.attachment

    override val transport: AuthenticatorTransport
        get() = setting.transport

    override fun makeCredential(
        hash:                            ByteArray,
        rpEntity:                        PublicKeyCredentialRpEntity,
        userEntity:                      PublicKeyCredentialUserEntity,
        requireResidentKey:              Boolean,
        requireUserPresence:             Boolean,
        requireUserVerification:         Boolean,
        credTypesAndPubKeyAlgs:          List<PublicKeyCredentialParameters>,
        excludeCredentialDescriptorList: List<PublicKeyCredentialDescriptor>
    ) {
        WAKLogger.d(TAG, "makeCredential")

        GlobalScope.launch {

            val requestedAlgorithms = credTypesAndPubKeyAlgs.map { it.alg }

            val keySupport = keySupportChooser.choose(requestedAlgorithms)
            if (keySupport == null) {
                WAKLogger.d(TAG, "supported alg not found, stop session")
                stop(ErrorReason.Unsupported)
                return@launch
            }

            val hasSourceToBeExcluded = excludeCredentialDescriptorList.any {
                credentialStore.lookupCredentialSource(it.id) != null
            }

            if (hasSourceToBeExcluded) {
                // TODO ask user to create new credential
                stop(ErrorReason.Unknown)
                return@launch
            }

            if (requireUserVerification && !setting.allowUserVerification) {
                stop(ErrorReason.Constraint)
                return@launch
            }


            val keyName = try {
                WAKLogger.d(TAG, "makeCredential - requestUserConsent")
                ui.requestUserConsent(
                    rpEntity                = rpEntity,
                    userEntity              = userEntity,
                    requireUserVerification = requireUserVerification
                )
            } catch(e: CancelledException) {
                WAKLogger.d(TAG, "makeCredential - requestUserConsent failure: $e")
                stop(ErrorReason.Cancelled)
                return@launch
            } catch(e: TimeoutException) {
                WAKLogger.d(TAG, "makeCredential - requestUserConsent failure: $e")
                stop(ErrorReason.Timeout)
                return@launch
            } catch(e: Exception) {
                WAKLogger.d(TAG, "makeCredential - requestUserConsent failure: $e")
                stop(ErrorReason.Unknown)
                return@launch
            }

            WAKLogger.d(TAG, "makeCredential - createNewCredentialId")
            val credentialId = createNewCredentialId()

            val rpId       = rpEntity.id!!
            val userHandle = userEntity.id

            WAKLogger.d(TAG, "makeCredential - create new credential source")

            val source = PublicKeyCredentialSource(
                signCount  = 0u,
                id         = credentialId,
                rpId       = rpId,
                userHandle = userHandle,
                alg        = keySupport.alg,
                otherUI    = keyName
            )

            credentialStore.deleteAllCredentialSources(
                rpId       = rpId,
                userHandle = userHandle
            )

            WAKLogger.d(TAG, "makeCredential - create new key pair")

            val pubKey = keySupport.createKeyPair(
                alias          = source.keyLabel,
                clientDataHash = hash
            )

            if (pubKey == null) {
                stop(ErrorReason.Unknown)
                return@launch
            }

            WAKLogger.d(TAG, "makeCredential - save credential source")

            if (!credentialStore.saveCredentialSource(source)) {
                stop(ErrorReason.Unknown)
                return@launch
            }

            WAKLogger.d(TAG, "makeCredential - create attested credential data")

            val attestedCredentialData = AttestedCredentialData(
                aaguid              = ByteArrayUtil.zeroUUIDBytes(),
                credentialId        = credentialId,
                credentialPublicKey = pubKey
            )

            val extensions = HashMap<String, Any>()

            val rpIdHash = ByteArrayUtil.sha256(rpId)

            WAKLogger.d(TAG, "makeCredential - create authenticator data")

            val authenticatorData = AuthenticatorData(
                rpIdHash               = rpIdHash,
                userPresent            = (requireUserPresence || requireUserVerification),
                userVerified           = requireUserVerification,
                signCount              = 0.toUInt(),
                attestedCredentialData = attestedCredentialData,
                extensions             = extensions
            )

            WAKLogger.d(TAG, "makeCredential - create attestation object")

            val attestation = keySupport.buildAttestationObject(
                alias             = source.keyLabel,
                authenticatorData = authenticatorData,
                clientDataHash    = hash
            )

            if (attestation == null) {
                stop(ErrorReason.Unknown)
                return@launch
            }

            onComplete()

            listener?.onCredentialCreated(
                this@InternalMakeCredentialSession, attestation)
        }

    }

    override fun canPerformUserVerification(): Boolean {
        WAKLogger.d(TAG, "canPerformUserVerification")
        return true
    }

    override fun canStoreResidentKey(): Boolean {
        WAKLogger.d(TAG, "canStoreResidentKey")
        return true
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
            WAKLogger.d(TAG, "UI is open")
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

    private fun createNewCredentialId(): ByteArray {
        return ByteArrayUtil.fromUUID(UUID.randomUUID())
    }

}