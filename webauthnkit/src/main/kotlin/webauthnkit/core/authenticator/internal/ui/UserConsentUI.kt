package webauthnkit.core.authenticator.internal.ui

import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import webauthnkit.core.CancelledException
import webauthnkit.core.PublicKeyCredentialRpEntity
import webauthnkit.core.PublicKeyCredentialUserEntity
import webauthnkit.core.UnknownException
import webauthnkit.core.authenticator.internal.PublicKeyCredentialSource
import webauthnkit.core.util.WAKLogger

import java.util.concurrent.Executors

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
class UserConsentUI(
    private val activity: FragmentActivity
) {
    companion object {
        val TAG = this::class.simpleName
    }

    var biometricPromptTitle         = "Title"
    var biometricPromptSubtitle      = "Subtitle"
    var biometricPromptDescription   = "Description"
    var biometricNegativeButtonTitle = "Cancel"

    private val executor = Executors.newSingleThreadExecutor()

    suspend fun requestUserConsent(
        rpEntity:                PublicKeyCredentialRpEntity,
        userEntity:              PublicKeyCredentialUserEntity,
        requireUserVerification: Boolean
    ): String = suspendCoroutine { cont ->

        GlobalScope.launch(Dispatchers.Unconfined) {

            // TODO
            // let user to confirm site information and create key name

            val newKeyName = userEntity.displayName

            if (requireUserVerification) {

                if (isFingerprintAvailable()) {

                    showBiometricPrompt(newKeyName, cont)

                } else {

                    // TODO fallback
                    // Passcode with Keyguard manager
                    cont.resumeWithException(CancelledException())

                }

            } else {

                cont.resume(newKeyName)

            }

        }

    }

    suspend fun requestUserSelection(
        sources:                 List<PublicKeyCredentialSource>,
        requireUserVerification: Boolean
    ): PublicKeyCredentialSource = suspendCoroutine { cont ->

        GlobalScope.launch(Dispatchers.Unconfined) {

            // TODO
            // request user to select key from list
            val selectedSource = sources[0]

            if (requireUserVerification) {

                if (isFingerprintAvailable()) {

                    showBiometricPrompt(selectedSource, cont)

                } else {

                    // TODO fallback
                    // Passcode with Keyguard manager
                    cont.resumeWithException(CancelledException())
                }

            } else {

                cont.resume(selectedSource)

            }

        }

    }

    private fun isFingerprintAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isFingerprintAvailableM()
        } else {
            isFingerprintAvailableN()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun isFingerprintAvailableM(): Boolean {
        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }

    private fun isFingerprintAvailableN(): Boolean {
        return false
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun <T> showBiometricPrompt(consentResult: T, cont: Continuation<T>) {

        val info =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(biometricPromptTitle)
                .setSubtitle(biometricPromptSubtitle)
                .setDescription(biometricPromptDescription)
                .setNegativeButtonText(biometricNegativeButtonTitle)
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
                cont.resumeWithException(UnknownException())
            }

        }).authenticate(info)

    }
}
