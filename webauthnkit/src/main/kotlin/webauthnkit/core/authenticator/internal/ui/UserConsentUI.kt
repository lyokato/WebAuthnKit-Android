package webauthnkit.core.authenticator.internal.ui

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.app.KeyguardManager
import android.content.Context.KEYGUARD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException

import kotlinx.coroutines.ExperimentalCoroutinesApi
import webauthnkit.core.*

import webauthnkit.core.authenticator.internal.PublicKeyCredentialSource
import webauthnkit.core.authenticator.internal.ui.dialog.*
import webauthnkit.core.util.WAKLogger
import java.util.*
import java.util.concurrent.Executor

interface KeyguardResultListener {
    fun onAuthenticated()
    fun onFailed()
}

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
object UserConsentUIFactory {
    val TAG = UserConsentUIFactory::class.simpleName
    fun create(activity: FragmentActivity): UserConsentUI {
        WAKLogger.d(TAG, "create")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            DefaultUserConsentUI(activity)
        } else {
            DefaultUserConsentUI(activity)
            //LegacyUserConsentUI(activity)
        }
    }
}

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
interface UserConsentUI {

    val isOpen: Boolean

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean

    fun cancel(reason: ErrorReason)

    suspend fun requestUserConsent(
        rpEntity:                PublicKeyCredentialRpEntity,
        userEntity:              PublicKeyCredentialUserEntity,
        requireUserVerification: Boolean
    ): String

    suspend fun requestUserSelection(
        sources:                 List<PublicKeyCredentialSource>,
        requireUserVerification: Boolean
    ): PublicKeyCredentialSource

}

// TODO ConsentUI for Android 5

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@TargetApi(Build.VERSION_CODES.M)
class DefaultUserConsentUI(
    private val activity: FragmentActivity
): UserConsentUI {

    companion object {
        val TAG = DefaultUserConsentUI::class.simpleName
        const val REQUEST_CODE = 6749
    }

    val uuid = UUID.randomUUID().toString()

    var keyguardResultListener: KeyguardResultListener? = null

    var biometricPromptCreateKeyTitle   = "Create key?"
    var biometricPromptSelectKeyTitle   = "Use this key?"
    var biometricPromptCancelButtonText = "CANCEL"

    var alwaysShowKeySelection: Boolean = false
    var preferBiometricPrompt: Boolean = false

    private val executor = MainThreadExecutor()

    override var isOpen: Boolean = false
        private set

    private var cancelled: ErrorReason? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {

        WAKLogger.d(TAG, "onActivityResult")

        return if (requestCode == REQUEST_CODE) {

            WAKLogger.d(TAG, "This is my result: $uuid")

            keyguardResultListener?.let {
                if (resultCode == RESULT_OK) {
                    WAKLogger.d(TAG, "OK")
                    it.onAuthenticated()
                } else {
                    WAKLogger.d(TAG, "Failed")
                    it.onFailed()
                }
            }

            keyguardResultListener = null

            true

        } else {

            false

        }
    }

    private fun onStartUserInteraction() {
        isOpen = true
        cancelled = null
    }

    private fun <T> finish(cont: Continuation<T>, result: T) {
        WAKLogger.d(TAG, "finish: $uuid")
        isOpen = false
        if (cancelled != null) {
            cont.resumeWithException(cancelled!!.rawValue)
        } else {
            cont.resume(result)
        }
    }

    private fun <T> fail(cont: Continuation<T>) {
        WAKLogger.d(TAG, "fail: $uuid")
        isOpen = false
        if (cancelled != null) {
            cont.resumeWithException(cancelled!!.rawValue)
        } else {
            cont.resumeWithException(CancelledException())
        }
    }

    override fun cancel(reason: ErrorReason) {
        cancelled = reason
    }

    override suspend fun requestUserConsent(
        rpEntity:                PublicKeyCredentialRpEntity,
        userEntity:              PublicKeyCredentialUserEntity,
        requireUserVerification: Boolean
    ): String = suspendCoroutine { cont ->

        WAKLogger.d(TAG, "requestUserConsent")

        onStartUserInteraction()

        activity.runOnUiThread {

            WAKLogger.d(TAG, "requestUserConsent switched to UI thread")

            // TODO make this configurable
            val dialog = DefaultRegistrationConfirmationDialog()
            dialog.show(activity, rpEntity, userEntity, object :
                RegistrationConfirmationDialogListener {

                override fun onCreate(keyName: String) {
                    if (requireUserVerification) {
                        if (isFingerprintAvailable() && preferBiometricPrompt) {
                            showBiometricPrompt(biometricPromptCreateKeyTitle, keyName, cont)
                        } else {
                            showKeyguard(cont, keyName)
                        }
                    } else {
                        finish(cont, keyName)
                    }
                }

                override fun onCancel() {
                    fail(cont)
                }

            })

        }
    }

    override suspend fun requestUserSelection(
        sources:                 List<PublicKeyCredentialSource>,
        requireUserVerification: Boolean
    ): PublicKeyCredentialSource = suspendCoroutine { cont ->

        WAKLogger.d(TAG, "requestUserSelection")

        onStartUserInteraction()

        activity.runOnUiThread {

            if (sources.size == 1 && !alwaysShowKeySelection) {

                WAKLogger.d(TAG, "found 1 source, skip selection")

                executeSelectionVerificationIfNeeded(
                    requireUserVerification = requireUserVerification,
                    source                  = sources[0],
                    cont                    = cont
                )

            } else {

                WAKLogger.d(TAG, "show selection dialog")

                val dialog = DefaultSelectionConfirmationDialog()

                dialog.show(activity, sources, object :
                    SelectionConfirmationDialogListener {

                    override fun onSelect(source: PublicKeyCredentialSource) {
                        WAKLogger.d(TAG, "selected")
                        executeSelectionVerificationIfNeeded(
                            requireUserVerification = requireUserVerification,
                            source                  = source,
                            cont                    = cont
                        )
                    }

                    override fun onCancel() {
                        WAKLogger.d(TAG, "canceled")
                        fail(cont)
                    }
                })

            }
        }
    }

    private fun executeSelectionVerificationIfNeeded(
        requireUserVerification: Boolean,
        source:                  PublicKeyCredentialSource,
        cont:                    Continuation<PublicKeyCredentialSource>
    ) {
        if (requireUserVerification) {
            if (isFingerprintAvailable() && preferBiometricPrompt) {
                showBiometricPrompt(biometricPromptSelectKeyTitle, source, cont)
            } else {
                showKeyguard(cont, source)
            }
        } else {
            finish(cont, source)
        }
    }

    private fun <T> showKeyguard(cont: Continuation<T>, consentResult: T) {

        WAKLogger.d(TAG, "showKeyguard")

        val keyguardManager =
            activity.getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        if (!keyguardManager.isKeyguardSecure) {
            WAKLogger.d(TAG, "keyguard is not secure")

            // TODO show error dialog
            fail(cont)

        } else {
            WAKLogger.d(TAG, "keyguard is secure")

            WAKLogger.d(TAG, "keyguard listener: $uuid")

            keyguardResultListener = object : KeyguardResultListener {

                override fun onAuthenticated() {
                    WAKLogger.d(TAG, "keyguard authenticated")
                    finish(cont, consentResult)
                }

                override fun onFailed() {
                    // TODO show error dialog
                    WAKLogger.d(TAG, "failed keyguard authentication")
                    fail(cont)
                }
            }

            val intent =
                keyguardManager.createConfirmDeviceCredentialIntent(null, null)
            activity.startActivityForResult(intent, REQUEST_CODE)
        }
    }

    private fun isFingerprintAvailable(): Boolean {
        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }

    private fun <T> showBiometricPrompt(title: String, consentResult: T, cont: Continuation<T>) {

        WAKLogger.d(TAG, "showBiometricPrompt")

        val info =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(biometricPromptCancelButtonText)
                .build()

        BiometricPrompt(activity, executor, object: BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                WAKLogger.d(TAG, "authentication success $uuid")
                finish(cont, consentResult)
            }

            override fun onAuthenticationFailed() {
                WAKLogger.d(TAG, "authentication failed")
                fail(cont)
            }

            @SuppressLint("SwitchIntDef")
            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                WAKLogger.w(TAG, "authentication error $uuid - $code: $msg")
                // TODO make these message configurable
                // show ErrorReasonDialog except for USER_CANCELED
                val reason = when(code) {
                    BiometricPrompt.ERROR_USER_CANCELED     -> "USER_CANCELED"
                    BiometricPrompt.ERROR_CANCELED          -> "CANCELED"
                    BiometricPrompt.ERROR_HW_NOT_PRESENT    -> "HW_NOT_PRESENT"
                    BiometricPrompt.ERROR_HW_UNAVAILABLE    -> "HW_UNAVAILABLE"
                    BiometricPrompt.ERROR_LOCKOUT           -> "LOCKOUT"
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "LOCKOUT_PERMANENT"
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON   -> "NEGATIVE_BUTTON"
                    BiometricPrompt.ERROR_NO_BIOMETRICS     -> "NO_BIOMETRICS"
                    BiometricPrompt.ERROR_NO_SPACE          -> "NO_SPACE"
                    BiometricPrompt.ERROR_TIMEOUT           -> "TIMEOUT"
                    BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> "UNABLE_TO_PROCESS"
                    BiometricPrompt.ERROR_VENDOR            -> "VENDOR"
                    else -> "UNKNOWN"
                }
                fail(cont)
                //showKeyguardFallbackDialog(reason, cont, consentResult)
            }

        }).authenticate(info)

    }

    private fun <T> showKeyguardFallbackDialog(reason: String, cont: Continuation<T>, consentResult: T) {

        WAKLogger.d(TAG, "showKeyguardFallbackDialog $uuid")

        activity.runOnUiThread {

            val dialog = KeyguardFallbackDialog()
            dialog.show(activity, reason, object :
                KeyguardFallbackDialogListener {

                override fun onRequest() {
                    showKeyguard(cont, consentResult)
                }

                override fun onCancel() {
                    fail(cont)
                }

            })
        }
    }

    inner class MainThreadExecutor : Executor {

        private val handler = Handler(Looper.getMainLooper())

        override fun execute(runnable: Runnable) {
            handler.post(runnable)
        }
    }
}
