package webauthnkit.core.client.operation

import java.util.*

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import webauthnkit.core.data.*
import webauthnkit.core.authenticator.AuthenticatorAssertionResult
import webauthnkit.core.authenticator.GetAssertionSession
import webauthnkit.core.authenticator.GetAssertionSessionListener
import webauthnkit.core.error.BadOperationException
import webauthnkit.core.error.ErrorReason
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.util.ByteArrayUtil

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class GetOperation(
    private val options: PublicKeyCredentialRequestOptions,
    private val rpId:           String,
    private val session:        GetAssertionSession,
    private val clientData:     CollectedClientData,
    private val clientDataJSON: String,
    private val clientDataHash: ByteArray,
    private val lifetimeTimer:  Long
) {

    companion object {
        val TAG = GetOperation::class.simpleName
    }

    val opId: String = UUID.randomUUID().toString()
    var listener: OperationListener? = null

    private var stopped: Boolean = false
    private var savedCredentialId: ByteArray? = null

    private val sessionListener = object : GetAssertionSessionListener {

        override fun onAvailable(session: GetAssertionSession) {
            WAKLogger.d(TAG, "onAvailable")

            if (stopped) {
                WAKLogger.d(TAG, "already stopped")
                return
            }

            if (options.userVerification == UserVerificationRequirement.Required
                && !session.canPerformUserVerification()) {
                WAKLogger.w(TAG, "user verification required, but this authenticator doesn't support")
                stop(ErrorReason.Unsupported)
                return
            }

            val userVerification = judgeUserVerificationExecution(session)

            val userPresence = !userVerification

            if (options.allowCredential.isEmpty()) {

                session.getAssertion(
                    rpId                          = rpId,
                    hash                          = clientDataHash,
                    allowCredentialDescriptorList = options.allowCredential,
                    requireUserVerification       = userVerification,
                    requireUserPresence           = userPresence
                )

            } else {

                val allowDescriptorList =
                    options.allowCredential.filter {
                        it.transports.contains(session.transport)
                    }

                if (allowDescriptorList.isEmpty()) {
                    WAKLogger.d(TAG, "no matched credentials exists on this authenticator")
                    stop(ErrorReason.NotAllowed)
                    return
                }

                if (allowDescriptorList.size == 1) {
                    savedCredentialId = allowDescriptorList[0].id
                }

                session.getAssertion(
                    rpId                          = rpId,
                    hash                          = clientDataHash,
                    allowCredentialDescriptorList = allowDescriptorList,
                    requireUserVerification       = userVerification,
                    requireUserPresence           = userPresence
                )

            }
        }

        override fun onCredentialDiscovered(session: GetAssertionSession,
                                            assertion: AuthenticatorAssertionResult) {
            WAKLogger.d(TAG, "onCredentialDiscovered")


            val credId = if (savedCredentialId != null) {
                WAKLogger.d(TAG, "onCredentialDiscovered - use saved credId")
                savedCredentialId
            } else {
                WAKLogger.d(TAG, "onCredentialDiscovered - use selected credId")
                val selectedCredId = assertion.credentialId
                if (selectedCredId == null) {
                    WAKLogger.w(TAG, "selected credential Id not found")
                    stop(ErrorReason.Unknown)
                    return
                }
                selectedCredId
            }

            WAKLogger.d(TAG, "onCredentialDiscovered - create assertion response")

            val response = AuthenticatorAssertionResponse(
                clientDataJSON    = clientDataJSON,
                authenticatorData = assertion.authenticatorData,
                signature         = assertion.signature,
                userHandle        = assertion.userHandle
            )

            WAKLogger.d(TAG, "onCredentialDiscovered - create credential")

            val cred = PublicKeyCredential(
                rawId    = credId!!,
                id       = ByteArrayUtil.encodeBase64URL(credId),
                response = response
            )

            completed()

            WAKLogger.d(TAG, "onCredentialDiscovered - resume")
            continuation?.resume(cred)
            continuation = null

        }

        override fun onOperationStopped(session: GetAssertionSession, reason: ErrorReason) {
            WAKLogger.d(TAG, "onOperationStopped")
            stop(reason)
        }

        override fun onUnavailable(session: GetAssertionSession) {
            WAKLogger.d(TAG, "onUnavailable")
            stop(ErrorReason.NotAllowed)
        }
    }

    private var continuation: Continuation<GetAssertionResponse>? = null

    suspend fun start(): GetAssertionResponse = suspendCoroutine { cont ->

        WAKLogger.d(TAG, "start")

        GlobalScope.launch {

            if (stopped) {
                WAKLogger.d(TAG, "already stopped")
                cont.resumeWithException(BadOperationException())
                listener?.onFinish(OperationType.Get, opId)
                return@launch
            }

            if (continuation != null) {
                WAKLogger.d(TAG, "continuation already exists")
                cont.resumeWithException(BadOperationException())
                listener?.onFinish(OperationType.Get, opId)
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
        listener?.onFinish(OperationType.Get, opId)
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
        listener?.onFinish(OperationType.Get, opId)
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

    private fun judgeUserVerificationExecution(session: GetAssertionSession): Boolean {
        WAKLogger.d(CreateOperation.TAG, "judgeUserVerificationExecution")

        return when (options.userVerification) {
            UserVerificationRequirement.Required    -> true
            UserVerificationRequirement.Discouraged -> false
            UserVerificationRequirement.Preferred   -> session.canPerformUserVerification()
        }
    }

}
