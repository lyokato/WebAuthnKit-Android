package webauthnkit.core.authenticator.internal.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ImplicitReflectionSerializer

import webauthnkit.core.*
import webauthnkit.core.authenticator.AttestedCredentialData
import webauthnkit.core.authenticator.AuthenticatorData
import webauthnkit.core.authenticator.MakeCredentialSession
import webauthnkit.core.authenticator.MakeCredentialSessionListener
import webauthnkit.core.authenticator.internal.CredentialStore
import webauthnkit.core.authenticator.internal.InternalAuthenticatorSetting
import webauthnkit.core.authenticator.internal.KeySupportChooser
import webauthnkit.core.authenticator.internal.PublicKeyCredentialSource
import webauthnkit.core.authenticator.internal.ui.UserConsentUI
import webauthnkit.core.util.AuthndroidLogger
import webauthnkit.core.util.ByteArrayUtil
import java.util.*

@ExperimentalCoroutinesApi
@ImplicitReflectionSerializer
@ExperimentalUnsignedTypes
class InternalMakeCredentialSession(
    private val setting:           InternalAuthenticatorSetting,
    private val ui:                UserConsentUI,
    private val credentialStore:   CredentialStore,
    private val keySupportChooser: KeySupportChooser
) : MakeCredentialSession {

    companion object {
        val TAG = this::class.simpleName
    }

    private var stopped = false
    private var started = false

    override var listener: MakeCredentialSessionListener? = null

    override val attachment: AuthenticatorAttachment
        get() = setting.attachment

    override val transport: AuthenticatorTransport
        get() = setting.transport

    override fun makeCredential(
        hash:                            UByteArray,
        rpEntity: PublicKeyCredentialRpEntity,
        userEntity: PublicKeyCredentialUserEntity,
        requireResidentKey:              Boolean,
        requireUserPresence:             Boolean,
        requireUserVerification:         Boolean,
        credTypesAndPubKeyAlgs:          List<PublicKeyCredentialParameters>,
        excludeCredentialDescriptorList: List<PublicKeyCredentialDescriptor>
    ) {
        AuthndroidLogger.d(TAG, "makeCredential")

        GlobalScope.launch {

            val requestedAlgorithms = credTypesAndPubKeyAlgs.map { it.alg }

            val keySupport = keySupportChooser.choose(requestedAlgorithms)
            if (keySupport == null) {
                AuthndroidLogger.d(TAG, "supported alg not found, stop session")
                stop(ErrorReason.Unsupported)
                return@launch
            }

            val hasSourceToBeExcluded = excludeCredentialDescriptorList.any {
                credentialStore.lookupCredentialSource(it.id.toByteArray()) != null
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
                ui.requestUserConsent(
                    rpEntity                = rpEntity,
                    userEntity              = userEntity,
                    requireUserVerification = requireUserVerification
                )
            } catch(e: Exception) {
                // TODO classify error
                stop(ErrorReason.Cancelled)
                return@launch
            }

            val credentialId = createNewCredentialId()

            val rpId       = rpEntity.id!!
            val userHandle = userEntity.id!!.toByteArray()

            val source = PublicKeyCredentialSource(
                signCount  = 0,
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

            val pubKey = keySupport.createKeyPair(
                alias          = source.keyLabel,
                clientDataHash = hash.toByteArray()
            )

            if (pubKey == null) {
                stop(ErrorReason.Unknown)
                return@launch
            }

            if (!credentialStore.saveCredentialSource(source)) {
                stop(ErrorReason.Unknown)
                return@launch
            }

            val attestedCredentialData = AttestedCredentialData(
                aaguid              = ByteArrayUtil.zeroUUIDBytes().toUByteArray(),
                credentialId        = credentialId.toUByteArray(),
                credentialPublicKey = pubKey
            )

            val extensions = HashMap<String, Any>()

            val rpIdHash = ByteArrayUtil.sha256(rpId)

            val authenticatorData = AuthenticatorData(
                rpIdHash               = rpIdHash.toUByteArray(),
                userPresent            = requireUserPresence,
                userVerified           = requireUserVerification,
                signCount              = 0.toUInt(),
                attestedCredentialData = attestedCredentialData,
                extensions             = extensions
            )

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
        AuthndroidLogger.d(TAG, "canPerformUserVerification")
        return true
    }

    override fun canStoreResidentKey(): Boolean {
        AuthndroidLogger.d(TAG, "canStoreResidentKey")
        return true
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

    private fun createNewCredentialId(): ByteArray {
        return ByteArrayUtil.fromUUID(UUID.randomUUID())
    }

}