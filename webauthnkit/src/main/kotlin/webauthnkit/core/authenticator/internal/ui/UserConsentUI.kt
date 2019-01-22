package webauthnkit.core.authenticator.internal.ui

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import webauthnkit.core.PublicKeyCredentialRpEntity
import webauthnkit.core.PublicKeyCredentialUserEntity
import webauthnkit.core.authenticator.internal.PublicKeyCredentialSource
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
class UserConsentUI(
    val activity: Activity
) {
    companion object {
        val TAG = this::class.simpleName
    }

    suspend fun requestUserConsent(
        rpEntity:                PublicKeyCredentialRpEntity,
        userEntity:              PublicKeyCredentialUserEntity,
        requireUserVerification: Boolean
    ): String = suspendCoroutine { cont ->

        GlobalScope.launch(Dispatchers.Unconfined) {
            // TODO
            // Show popup dialog upon activity
            cont.resume(userEntity.displayName)
        }

    }

    suspend fun requestUserSelection(
        sources:                 List<PublicKeyCredentialSource>,
        requireUserVerification: Boolean
    ): PublicKeyCredentialSource = suspendCoroutine { cont ->

        GlobalScope.launch(Dispatchers.Unconfined) {
            // TODO
            // Show popup dialog upon activity
            cont.resume(sources[0])
        }

    }
}
