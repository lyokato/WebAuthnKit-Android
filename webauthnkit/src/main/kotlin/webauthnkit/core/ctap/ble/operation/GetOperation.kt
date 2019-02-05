package webauthnkit.core.ctap.ble.operation

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
import webauthnkit.core.client.operation.OperationListener
import webauthnkit.core.client.operation.OperationType
import webauthnkit.core.error.BadOperationException
import webauthnkit.core.error.ErrorReason
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.CBORWriter

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class GetOperation(
    private val rpId:                    String,
    private val clientDataHash:          ByteArray,
    private val allowCredential:         List<PublicKeyCredentialDescriptor>,
    private val requireUserVerification: Boolean,
    private val requireUserPresence:     Boolean,
    private val session:                 GetAssertionSession,
    private val lifetimeTimer:           Long
) {

    companion object {
        val TAG = GetOperation::class.simpleName
    }

    val allowListSize = allowCredential.size

    val opId: String = UUID.randomUUID().toString()
    var listener: OperationListener? = null

    private var stopped: Boolean = false

    private val sessionListener = object : GetAssertionSessionListener {

        override fun onAvailable(session: GetAssertionSession) {
            WAKLogger.d(TAG, "onAvailable")

            if (stopped) {
                WAKLogger.d(TAG, "already stopped")
                return
            }

            if (requireUserVerification && !session.canPerformUserVerification()) {
                WAKLogger.w(TAG, "user verification required, but this authenticator doesn't support")
                stop(ErrorReason.Unsupported)
                return
            }

            session.getAssertion(
                rpId                          = rpId,
                hash                          = clientDataHash,
                allowCredentialDescriptorList = allowCredential,
                requireUserVerification       = requireUserVerification,
                requireUserPresence           = requireUserPresence
            )

        }

        override fun onCredentialDiscovered(
            session:   GetAssertionSession,
            assertion: AuthenticatorAssertionResult
        ) {

            WAKLogger.d(TAG, "onCredentialDiscovered")

            // TODO UserEntityのidの型をByteArrayにする?

            val user = mutableMapOf<String, Any>()
            user["id"] = assertion.userHandle!!

            val map = mutableMapOf<String, Any>()
            map["authData"]            = assertion.authenticatorData
            map["signature"]           = assertion.signature
            map["user"]                = user
            map["numberOfCredentials"] = 1L

            if (allowListSize != 1) {
                WAKLogger.d(TAG, "onCredentialDiscovered - use selected credId")
                val selectedCredId = assertion.credentialId
                if (selectedCredId == null) {
                    WAKLogger.w(TAG, "selected credential Id not found")
                    stop(ErrorReason.Unknown)
                    return
                }

                val cred = mutableMapOf<String, Any>()
                cred["type"] = "public-key"
                cred["id"]   = selectedCredId

                map["credential"] = cred
            }

            val result = CBORWriter().putStringKeyMap(map).compute()

            completed()

            WAKLogger.d(TAG, "onCredentialDiscovered - resume")
            continuation?.resume(result)
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

    private var continuation: Continuation<ByteArray>? = null

    suspend fun start(): ByteArray = suspendCoroutine { cont ->

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

}
