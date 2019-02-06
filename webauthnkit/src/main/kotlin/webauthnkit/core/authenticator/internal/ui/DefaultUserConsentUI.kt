package webauthnkit.core.authenticator.internal.ui

import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.app.KeyguardManager
import android.content.Context.KEYGUARD_SERVICE
import android.content.Intent
import android.os.Build
import androidx.fragment.app.FragmentActivity

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException

import kotlinx.coroutines.ExperimentalCoroutinesApi

import webauthnkit.core.authenticator.internal.PublicKeyCredentialSource
import webauthnkit.core.authenticator.internal.ui.dialog.*
import webauthnkit.core.error.*
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.data.*

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

    var keyguardResultListener: KeyguardResultListener? = null

    override val config = UserConsentUIConfig()

    override var isOpen: Boolean = false
        private set

    private var cancelled: ErrorReason? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {

        WAKLogger.d(TAG, "onActivityResult")

        return if (requestCode == REQUEST_CODE) {

            WAKLogger.d(TAG, "This is my result")

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
        WAKLogger.d(TAG, "finish")
        isOpen = false
        if (cancelled != null) {
            cont.resumeWithException(cancelled!!.rawValue)
        } else {
            cont.resume(result)
        }
    }

    private fun <T> fail(cont: Continuation<T>) {
        WAKLogger.d(TAG, "fail")
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
            val dialog = DefaultRegistrationConfirmationDialog(config)

            dialog.show(activity, rpEntity, userEntity, object :
                RegistrationConfirmationDialogListener {

                override fun onCreate(keyName: String) {
                    if (requireUserVerification) {
                        showKeyguard(cont, keyName)
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

            if (sources.size == 1 && !config.alwaysShowKeySelection) {

                WAKLogger.d(TAG, "found 1 source, skip selection")

                executeSelectionVerificationIfNeeded(
                    requireUserVerification = requireUserVerification,
                    source                  = sources[0],
                    cont                    = cont
                )

            } else {

                WAKLogger.d(TAG, "show selection dialog")

                val dialog = DefaultSelectionConfirmationDialog(config)

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
            showKeyguard(cont, source)
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

            showErrorDialog(cont, config.messageKeyguardNotSetError)

        } else {
            WAKLogger.d(TAG, "keyguard is secure")

            keyguardResultListener = object : KeyguardResultListener {

                override fun onAuthenticated() {
                    WAKLogger.d(TAG, "keyguard authenticated")
                    finish(cont, consentResult)
                }

                override fun onFailed() {
                    WAKLogger.d(TAG, "failed keyguard authentication")
                    fail(cont)
                }
            }

            val intent =
                keyguardManager.createConfirmDeviceCredentialIntent(
                    config.messageKeyguardTitle, config.messageKeyguardDescription)
            activity.startActivityForResult(intent, REQUEST_CODE)
        }
    }

    private fun <T> showErrorDialog(cont: Continuation<T>, reason: String) {

        val dialog = VerificationErrorDialog(config)

        dialog.show(activity, reason, object: VerificationErrorDialogListener {
            override fun onComplete() {
                fail(cont)
            }
        })

    }
}
