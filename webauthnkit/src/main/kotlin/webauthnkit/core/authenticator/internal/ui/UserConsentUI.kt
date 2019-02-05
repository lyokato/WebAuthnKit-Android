package webauthnkit.core.authenticator.internal.ui

import android.content.Intent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import webauthnkit.core.authenticator.internal.PublicKeyCredentialSource
import webauthnkit.core.error.ErrorReason
import webauthnkit.core.data.*

interface KeyguardResultListener {
    fun onAuthenticated()
    fun onFailed()
}

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
interface UserConsentUI {

    val config: UserConsentUIConfig

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

