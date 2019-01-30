package webauthnkit.core.authenticator.internal.ui

import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.app.KeyguardManager
import android.content.Context.KEYGUARD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException

import kotlinx.coroutines.ExperimentalCoroutinesApi
import webauthnkit.core.*

import webauthnkit.core.authenticator.internal.PublicKeyCredentialSource
import webauthnkit.core.util.WAKLogger

import java.util.concurrent.Executors

interface KeyguardResultListener {
    fun onAuthenticated()
    fun onFailed()
}

// TODO ConsentUI for Android 5

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@TargetApi(Build.VERSION_CODES.M)
class UserConsentUI(
    private val activity: FragmentActivity
) {
    companion object {
        val TAG = UserConsentUI::class.simpleName
        const val REQUEST_CODE = 6749
    }

    var keyguardResultListener: KeyguardResultListener? = null

    var biometricPromptCreateKeyTitle   = "Create key?"
    var biometricPromptSelectKeyTitle   = "Use this key?"
    var biometricPromptCancelButtonText = "CANCEL"

    var alwaysShowKeySelection: Boolean = false
    var preferBiometricPrompt: Boolean = true

    private val executor = Executors.newSingleThreadExecutor()

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        WAKLogger.d(TAG, "onActivityResult")
        return if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                keyguardResultListener?.onAuthenticated()
            } else {
                keyguardResultListener?.onFailed()
            }
            keyguardResultListener = null
            true
        } else {
            false
        }
    }

    suspend fun requestUserConsent(
        rpEntity:                PublicKeyCredentialRpEntity,
        userEntity:              PublicKeyCredentialUserEntity,
        requireUserVerification: Boolean
    ): String = suspendCoroutine { cont ->

        WAKLogger.d(TAG, "requestUserConsent")

        activity.runOnUiThread {

            WAKLogger.d(TAG, "requestUserConsent switched to UI thread")

            val dialog = DefaultRegistrationConfirmationDialog()
            dialog.show(activity, rpEntity, userEntity, object : RegistrationConfirmationDialogListener{

                override fun onCreate(keyName: String) {
                    if (requireUserVerification) {
                        if (isFingerprintAvailable() && preferBiometricPrompt) {
                            showBiometricPrompt(biometricPromptCreateKeyTitle, keyName, cont)
                        } else {
                            showKeyguard(cont, keyName)
                        }
                    } else {
                        cont.resume(keyName)
                    }
                }

                override fun onCancel() {
                    cont.resumeWithException(CancelledException())
                }

            })

        }
    }

    suspend fun requestUserSelection(
        sources:                 List<PublicKeyCredentialSource>,
        requireUserVerification: Boolean
    ): PublicKeyCredentialSource = suspendCoroutine { cont ->

        WAKLogger.d(TAG, "requestUserSelection")

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

                dialog.show(activity, sources, object : SelectionConfirmationDialogListener {

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
                        cont.resumeWithException(CancelledException())
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
            cont.resume(source)
        }
    }

    private fun <T> showKeyguard(cont: Continuation<T>, consentResult: T) {

        WAKLogger.d(TAG, "showKeyguard")

        val keyguardManager =
            activity.getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        if (!keyguardManager.isKeyguardSecure) {
            WAKLogger.d(TAG, "keyguard is not secure")

            // TODO show error dialog
            cont.resumeWithException(CancelledException())

        } else {
            WAKLogger.d(TAG, "keyguard is secure")

            keyguardResultListener = object : KeyguardResultListener {

                override fun onAuthenticated() {
                    cont.resume(consentResult)
                }

                override fun onFailed() {
                    // TODO show error dialog
                    cont.resumeWithException(CancelledException())
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
                WAKLogger.d(TAG, "authentication success")
                cont.resume(consentResult)
            }

            override fun onAuthenticationFailed() {
                WAKLogger.d(TAG, "authentication failed")
                cont.resumeWithException(CancelledException())
            }

            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                WAKLogger.w(TAG, "authentication error $code: $msg")
                //cont.resumeWithException(UnknownException())
                showKeyguard(cont, consentResult)
            }

        }).authenticate(info)

    }
}
