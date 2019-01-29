package webauthnkit.core.authenticator.internal.ui

import android.annotation.TargetApi
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.text.format.DateFormat
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import webauthnkit.core.*

import webauthnkit.core.authenticator.internal.PublicKeyCredentialSource
import webauthnkit.core.util.WAKLogger
import java.util.*

import java.util.concurrent.Executors

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
class UserConsentUI(
    private val activity: FragmentActivity
) {
    companion object {
        val TAG = UserConsentUI::class.simpleName
    }

    var biometricPromptCreateKeyTitle   = "Create key?"
    var biometricPromptSelectKeyTitle   = "Use this key?"
    var biometricPromptCancelButtonText = "CANCEL"

    private val executor = Executors.newSingleThreadExecutor()

    suspend fun requestUserConsent(
        rpEntity:                PublicKeyCredentialRpEntity,
        userEntity:              PublicKeyCredentialUserEntity,
        requireUserVerification: Boolean
    ): String = suspendCoroutine { cont ->

        WAKLogger.d(TAG, "requestUserConsent")

        activity.runOnUiThread {

            WAKLogger.d(TAG, "requestUserConsent switched to UI thread")

            val dialog = RegistrationConfirmationDialog(
                activity   = activity,
                rpEntity   = rpEntity,
                userEntity = userEntity
            )

            dialog.listener = object : RegistrationConfirmationDialogInterface {

                override fun onCreate(keyName: String) {

                    if (requireUserVerification) {

                        if (isFingerprintAvailable()) {

                            showBiometricPrompt(biometricPromptCreateKeyTitle, keyName, cont)

                        } else {

                            // TODO fallback
                            // Passcode with Keyguard manager
                            cont.resumeWithException(CancelledException())

                        }

                    } else {

                        cont.resume(keyName)

                    }

                }

                override fun onCancel() {

                    cont.resumeWithException(CancelledException())

                }
            }

            dialog.show()
        }
    }

    suspend fun requestUserSelection(
        sources:                 List<PublicKeyCredentialSource>,
        requireUserVerification: Boolean
    ): PublicKeyCredentialSource = suspendCoroutine { cont ->

        WAKLogger.d(TAG, "requestUserSelection")

        activity.runOnUiThread {

            // TODO
            // request user to select key from list
            val selectedSource = sources[0]

            if (requireUserVerification) {

                if (isFingerprintAvailable()) {

                    showBiometricPrompt(biometricPromptSelectKeyTitle, selectedSource, cont)

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
    private fun <T> showBiometricPrompt(title: String, consentResult: T, cont: Continuation<T>) {

        WAKLogger.d(TAG, "showBiometricPrompt")

        val info =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                //.setSubtitle(biometricPromptSubtitle)
                //.setDescription(biometricPromptDescription)
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
                cont.resumeWithException(UnknownException())
            }

        }).authenticate(info)

    }
}
