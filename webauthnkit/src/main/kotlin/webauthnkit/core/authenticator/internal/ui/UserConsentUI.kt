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

        WAKLogger.d(TAG, "requestUserConsent")

        activity.runOnUiThread {

            WAKLogger.d(TAG, "requestUserConsent switched to UI thread")
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

                showConfirmationDialog(rpEntity, userEntity)
                //cont.resume(newKeyName)

            }

        }
    }

    private fun showConfirmationDialog(
        rpEntity:   PublicKeyCredentialRpEntity,
        userEntity: PublicKeyCredentialUserEntity
    ) {

        val dialog = Dialog(activity)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.window!!.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        )

        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.setContentView(R.layout.webauthn_registration_conformation_dialog)

        dialog.findViewById<TextView>(R.id.webauthn_registration_username).text = userEntity.displayName

        val date = DateFormat.format("yyyyMMdd", Calendar.getInstance())
        val defaultKeyName = "${userEntity.name}($date)"

        dialog.findViewById<EditText>(R.id.webauthn_registration_key_name).setText(defaultKeyName)

        val rpName = "[ ${rpEntity.name} ]"
        dialog.findViewById<TextView>(R.id.webauthn_registration_rp).text = rpName

        val userIconView = dialog.findViewById<ImageView>(R.id.webauthn_registration_user_icon)

        userEntity.icon?.let {

            val radius = activity.resources.getDimensionPixelSize(R.dimen.user_icon_radius)

            val option= RequestOptions().let {

                it.fitCenter()
                it.transform(MultiTransformation(CenterCrop(), RoundedCorners(radius)))

            }
            Glide.with(activity)
                .load(userEntity.icon)
                .apply(option)
                .into(userIconView)
        }

        val rpIconView = dialog.findViewById<ImageView>(R.id.webauthn_registration_rp_icon)

        rpEntity.icon?.let {

            val radius = activity.resources.getDimensionPixelSize(R.dimen.rp_icon_radius)

            val option= RequestOptions().let {

                it.fitCenter()
                it.transform(MultiTransformation(CenterCrop(), RoundedCorners(radius)))
            }

            Glide.with(activity)
                .load(rpEntity.icon)
                .apply(option)
                .into(rpIconView)
        }

        dialog.findViewById<Button>(R.id.webauthn_registration_confirmation_cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.webauthn_registration_confirmation_ok_button).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
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

        WAKLogger.d(TAG, "showBiometricPrompt")

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
