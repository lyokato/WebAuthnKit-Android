package webauthnkit.core.client.operation

import java.util.*

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import webauthnkit.core.error.ErrorReason
import webauthnkit.core.data.*
import webauthnkit.core.error.*
import webauthnkit.core.authenticator.AttestationObject
import webauthnkit.core.authenticator.MakeCredentialSession
import webauthnkit.core.authenticator.MakeCredentialSessionListener
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.util.ByteArrayUtil

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class CreateOperation(
    private val options: PublicKeyCredentialCreationOptions,
    private val rpId:           String,
    private val session:        MakeCredentialSession,
    private val clientData:     CollectedClientData,
    private val clientDataJSON: String,
    private val clientDataHash: ByteArray,
    private val lifetimeTimer:  Long
) {

    companion object {
        val TAG = CreateOperation::class.simpleName
    }

    val opId: String = UUID.randomUUID().toString()
    var listener: OperationListener? = null

    private var stopped: Boolean = false

    private val sessionListener = object : MakeCredentialSessionListener {

        override fun onAvailable(session: MakeCredentialSession) {
            WAKLogger.d(TAG, "onAvailable")

            if (stopped) {
                WAKLogger.d(TAG, "already stopped")
                return
            }

            if (options.authenticatorSelection != null) {

                val selection = options.authenticatorSelection!!

                if (selection.authenticatorAttachment != null) {
                    if (selection.authenticatorAttachment != session.attachment) {
                        WAKLogger.d(TAG, "attachment doesn't match to RP's request")
                        stop(ErrorReason.Unsupported)
                        return
                    }
                }

                if (selection.requireResidentKey && !session.canStoreResidentKey()) {
                    WAKLogger.d(TAG, "This authenticator can't store resident-key")
                    stop(ErrorReason.Unsupported)
                    return
                }

                if (selection.userVerification == UserVerificationRequirement.Required
                    && !session.canPerformUserVerification()) {
                    WAKLogger.d(TAG, "This authenticator can't perform user verification")
                    stop(ErrorReason.Unsupported)
                    return
                }
            }

            val userVerification = judgeUserVerificationExecution(session)

            val userPresence = !userVerification

            val excludeCredentialDescriptorList =
                    options.excludeCredentials.filter {
                       it.transports.contains(session.transport)
                    }

            val requireResidentKey = options.authenticatorSelection?.requireResidentKey ?: false

            val rpEntity = PublicKeyCredentialRpEntity(
                id = rpId,
                name = options.rp.name,
                icon = options.rp.icon
            )

            session.makeCredential(
                hash                            = clientDataHash,
                rpEntity                        = rpEntity,
                userEntity                      = options.user,
                requireResidentKey              = requireResidentKey,
                requireUserPresence             = userPresence,
                requireUserVerification         = userVerification,
                credTypesAndPubKeyAlgs          = options.pubKeyCredParams,
                excludeCredentialDescriptorList = excludeCredentialDescriptorList
            )
        }

        override fun onCredentialCreated(session: MakeCredentialSession, attestationObject: AttestationObject) {
            WAKLogger.d(TAG, "onCredentialCreated")

            val attestedCred = attestationObject.authData.attestedCredentialData
            if (attestedCred == null) {
                WAKLogger.w(TAG, "attested credential data not found")
                dispatchError(ErrorReason.Unknown)
                return
            }

            val credId = attestedCred.credentialId

            val resultedAttestationObject: ByteArray?

            if (options.attestation == AttestationConveyancePreference.None
                && attestationObject.isSelfAttestation()) {
                // if it's self attestation,
                // embed 0x00 for aaguid, and empty CBOR map for AttStmt

                val bytes = attestationObject.toNone().toBytes()
                if (bytes == null) {
                    WAKLogger.w(TAG, "failed to build attestation object")
                    dispatchError(ErrorReason.Unknown)
                    return
                }

                resultedAttestationObject = bytes


                // replace AAGUID to null
                val guidPos = 37 // ( rpIdHash(32), flag(1), signCount(4) )
                for (idx in (guidPos..(guidPos+15))) {
                    resultedAttestationObject[idx] = 0x00.toByte()
                }

            } else {
                // if it's other attestation
                // encoded to byte array as it is
                val bytes = attestationObject.toBytes()
                if (bytes == null) {
                    WAKLogger.w(TAG, "failed to build attestation object")
                    dispatchError(ErrorReason.Unknown)
                    return
                }
                resultedAttestationObject = bytes
            }

            val response = AuthenticatorAttestationResponse(
                clientDataJSON    = clientDataJSON,
                attestationObject = resultedAttestationObject
            )

            val cred = PublicKeyCredential(
                rawId    = credId,
                id       = ByteArrayUtil.encodeBase64URL(credId),
                response = response
            )

            completed()

            continuation?.resume(cred)
            continuation = null

        }

        override fun onOperationStopped(session: MakeCredentialSession, reason: ErrorReason) {
            WAKLogger.d(TAG, "onOperationStopped")
            stop(reason)
        }

        override fun onUnavailable(session: MakeCredentialSession) {
            WAKLogger.d(TAG, "onUnavailable")
            stop(ErrorReason.NotAllowed)
        }

    }

    private var continuation: Continuation<MakeCredentialResponse>? = null

    suspend fun start(): MakeCredentialResponse = suspendCoroutine { cont ->

        WAKLogger.d(TAG, "start")

        GlobalScope.launch {

            if (stopped) {
                WAKLogger.d(TAG, "already stopped")
                cont.resumeWithException(BadOperationException())
                listener?.onFinish(OperationType.Create, opId)
                return@launch
            }

            if (continuation != null) {
                WAKLogger.d(TAG, "continuation already exists")
                cont.resumeWithException(BadOperationException())
                listener?.onFinish(OperationType.Create, opId)
                return@launch
            }

            continuation = cont

            startTimer()

            session.listener = sessionListener
            session.start()
        }
    }

    fun cancel(reason: ErrorReason = ErrorReason.Timeout) {
        WAKLogger.d(TAG, "cancel")
        if (continuation != null && !this.stopped) {
            GlobalScope.launch {
                when (session.transport) {
                    AuthenticatorTransport.Internal -> {
                        when (reason) {
                            ErrorReason.Timeout -> {
                                session.cancel(ErrorReason.Timeout)
                            }
                            else -> {
                                session.cancel(ErrorReason.Cancelled)
                            }
                        }
                    }
                    else -> {
                        stop(reason)
                    }
                }
            }
        }
    }

    private fun stop(reason: ErrorReason) {
        WAKLogger.d(TAG, "stop")
        stopInternal(reason)
        dispatchError(reason)
    }

    private fun completed() {
        WAKLogger.d(TAG, "completed")
        stopTimer()
        listener?.onFinish(OperationType.Create, opId)
    }

    private fun stopInternal(reason: ErrorReason) {
        WAKLogger.d(TAG, "stopInternal")
        if (continuation == null) {
            WAKLogger.d(TAG, "not started")
           // not started
            return
        }
        if (stopped) {
            WAKLogger.d(TAG, "already stopped")
            return
        }
        stopTimer()
        session.cancel(reason)
        listener?.onFinish(OperationType.Create, opId)
    }

    private fun dispatchError(reason: ErrorReason) {
        WAKLogger.d(TAG, "dispatchError")
        continuation?.resumeWithException(reason.rawValue)
    }

    private var timer: Timer? = null

    private fun startTimer() {
        WAKLogger.d(TAG, "startTimer")
        stopTimer()
        timer = Timer()
        timer!!.schedule(object: TimerTask(){
            override fun run() {
                timer = null
                onTimeout()
            }
        }, lifetimeTimer*1000)
    }

    private fun stopTimer() {
        WAKLogger.d(TAG, "stopTimer")
        timer?.cancel()
        timer = null
    }

    private fun onTimeout() {
        WAKLogger.d(TAG, "onTimeout")
        stopTimer()
        cancel(ErrorReason.Timeout)
    }

    private fun judgeUserVerificationExecution(session: MakeCredentialSession): Boolean {
        WAKLogger.d(TAG, "judgeUserVerificationExecution")

        val userVerificationRequest =
            options.authenticatorSelection?.userVerification
                ?: UserVerificationRequirement.Discouraged

        return when (userVerificationRequest) {
            UserVerificationRequirement.Required    -> true
            UserVerificationRequirement.Discouraged -> false
            UserVerificationRequirement.Preferred   -> session.canPerformUserVerification()
        }
    }
}
