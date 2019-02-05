package webauthnkit.core.authenticator.internal.ui

import android.annotation.TargetApi
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
import webauthnkit.core.error.ErrorReason
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.data.*
import webauthnkit.core.error.*

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@TargetApi(Build.VERSION_CODES.M)
class LegacyUserConsentUI(
    private val activity: FragmentActivity
): UserConsentUI {

    companion object {
        val TAG = LegacyUserConsentUI::class.simpleName
    }

    override val config = UserConsentUIConfig()

    override var isOpen: Boolean = false
        private set

    private var cancelled: ErrorReason? = null

    private fun onStartUserInteraction() {
        isOpen = true
        cancelled = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return false
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

            if (requireUserVerification) {

                showErrorDialog(cont, config.messageVerificationNotSupported)

            } else {

                val dialog = DefaultRegistrationConfirmationDialog(config)

                dialog.show(activity, rpEntity, userEntity, object :
                    RegistrationConfirmationDialogListener {

                    override fun onCreate(keyName: String) {
                        finish(cont, keyName)
                    }

                    override fun onCancel() {
                        fail(cont)
                    }

                })
            }
        }
    }

    override suspend fun requestUserSelection(
        sources:                 List<PublicKeyCredentialSource>,
        requireUserVerification: Boolean
    ): PublicKeyCredentialSource = suspendCoroutine { cont ->

        WAKLogger.d(TAG, "requestUserSelection")

        onStartUserInteraction()

        activity.runOnUiThread {

            if (requireUserVerification) {

                showErrorDialog(cont, config.messageVerificationNotSupported)

            } else {

                if (sources.size == 1 && !config.alwaysShowKeySelection) {

                    WAKLogger.d(TAG, "found 1 source, skip selection")
                    finish(cont, sources[0])

                } else {

                    WAKLogger.d(TAG, "show selection dialog")

                    val dialog = DefaultSelectionConfirmationDialog(config)

                    dialog.show(activity, sources, object :
                        SelectionConfirmationDialogListener {

                        override fun onSelect(source: PublicKeyCredentialSource) {
                            WAKLogger.d(TAG, "selected")
                            finish(cont, source)
                        }

                        override fun onCancel() {
                            WAKLogger.d(TAG, "canceled")
                            fail(cont)
                        }
                    })
                }
            }
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
