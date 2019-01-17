package webauthnkit.core.client.operation

import java.util.*

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import webauthnkit.core.*
import webauthnkit.core.authenticator.AuthenticatorAssertionResult
import webauthnkit.core.authenticator.GetAssertionSession
import webauthnkit.core.authenticator.GetAssertionSessionListener
import webauthnkit.core.util.AuthndroidLogger
import webauthnkit.core.util.ByteArrayUtil

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class GetOperation(
    private val options: PublicKeyCredentialRequestOptions,
    private val rpId:           String,
    private val session:        GetAssertionSession,
    private val clientData:     CollectedClientData,
    private val clientDataJSON: String,
    private val clientDataHash: UByteArray,
    private val lifetimeTimer:  Long
) {

    companion object {
        val TAG = this::class.simpleName
    }

    private var stopped: Boolean = false
    private var savedCredentialId: UByteArray? = null

    private val sessionListener = object : GetAssertionSessionListener {

        override fun onAvailable(session: GetAssertionSession) {
            AuthndroidLogger.d(TAG, "onAvailable")

            if (stopped) {
                AuthndroidLogger.d(TAG, "already stopped")
                return
            }

            if (options.userVerification == UserVerificationRequirement.Required
                && !session.canPerformUserVerification()) {
                AuthndroidLogger.w(TAG, "user verification required, but this authenticator doesn't support")
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

                val allowDescriptorList = options.allowCredential.filter {
                    it.transports.contains(session.transport)
                }

                if (allowDescriptorList.isEmpty()) {
                    AuthndroidLogger.d(TAG, "no matched credentials exists on this authenticator")
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

        override fun onCredentialDiscovered(session: GetAssertionSession, assertion: AuthenticatorAssertionResult) {
            AuthndroidLogger.d(TAG, "onCredentialCreated")


            val credId = if (savedCredentialId == null) {
                savedCredentialId
            } else {
                val selectedCredId = assertion.credentialId
                if (selectedCredId == null) {
                    AuthndroidLogger.w(TAG, "selected credential Id not found")
                    stop(ErrorReason.Unknown)
                    return
                }
                selectedCredId
            }

            val response = AuthenticatorAssertionResponse(
                clientDataJSON    = clientDataJSON,
                authenticatorData = assertion.authenticatorData,
                signature         = assertion.signature,
                userHandle        = assertion.userHandle
            )

            val cred = PublicKeyCredential(
                rawId    = credId!!,
                id       = ByteArrayUtil.encodeBase64URL(credId),
                response = response
            )

            completed()

            continuation?.resume(cred)
            continuation = null

        }

        override fun onOperationStopped(session: GetAssertionSession, reason: ErrorReason) {
            AuthndroidLogger.d(TAG, "onOperationStopped")
        }

        override fun onUnavailable(session: GetAssertionSession) {
            AuthndroidLogger.d(TAG, "onUnavailable")
            stop(ErrorReason.NotAllowed)
        }
    }

    private var continuation: Continuation<GetAssertionResponse>? = null

    suspend fun start(): GetAssertionResponse = suspendCoroutine { cont ->

        AuthndroidLogger.d(TAG, "start")

        GlobalScope.launch {
            if (stopped) {
                AuthndroidLogger.d(TAG, "already stopped")
                cont.resumeWithException(BadOperationException())
                return@launch
            }

            continuation = cont

            startTimer()

            session.listener = sessionListener
            session.start()
        }
    }

    fun cancel() {
        AuthndroidLogger.d(TAG, "cancel")
    }

    private fun stop(reason: ErrorReason) {
        AuthndroidLogger.d(TAG, "stop")
        stopInternal(reason)
        dispatchError(reason)
    }

    private fun completed() {
        AuthndroidLogger.d(TAG, "completed")
        stopTimer()
    }

    private fun stopInternal(reason: ErrorReason) {
        AuthndroidLogger.d(TAG, "stopInternal")
        if (continuation == null) {
            AuthndroidLogger.d(TAG, "not started")
            // not started
            return
        }
        if (stopped) {
            AuthndroidLogger.d(TAG, "already stopped")
            return
        }
        stopTimer()
        session.cancel(reason)
        // listener!.onFinish()
    }

    private fun dispatchError(reason: ErrorReason) {
        AuthndroidLogger.d(TAG, "dispatchError")
        GlobalScope.launch(Dispatchers.Unconfined) {
            continuation?.resumeWithException(reason.rawValue)
        }
    }

    private var timer: Timer? = null

    private fun startTimer() {
        AuthndroidLogger.d(TAG, "startTimer")
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
        AuthndroidLogger.d(TAG, "stopTimer")
        timer?.cancel()
        timer = null
    }

    private fun onTimeout() {
        AuthndroidLogger.d(TAG, "onTimeout")
        stop(ErrorReason.Timeout)
    }

    private fun judgeUserVerificationExecution(session: GetAssertionSession): Boolean {
        AuthndroidLogger.d(CreateOperation.TAG, "judgeUserVerificationExecution")

        return when (options.userVerification) {
            UserVerificationRequirement.Required    -> true
            UserVerificationRequirement.Discouraged -> false
            UserVerificationRequirement.Preferred   -> session.canPerformUserVerification()
        }
    }

}
