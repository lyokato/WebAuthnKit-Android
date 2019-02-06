package webauthnkit.core.ctap.response

import webauthnkit.core.authenticator.AuthenticatorAssertionResult
import webauthnkit.core.error.ErrorReason
import webauthnkit.core.util.CBORWriter
import webauthnkit.core.util.WAKLogger
import java.lang.Exception

@ExperimentalUnsignedTypes
class GetAssertionResponseBuilder(
    private val assertion:     AuthenticatorAssertionResult,
    private val allowListSize: Int
) {

    companion object {
        val TAG = GetAssertionResponseBuilder::class.simpleName
    }

    fun build(): Pair<ByteArray?, ErrorReason?> {
        WAKLogger.d(TAG, "build")

        try {

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
                    return Pair(null, ErrorReason.Unknown)
                }

                val cred = mutableMapOf<String, Any>()
                cred["type"] = "public-key"
                cred["id"]   = selectedCredId

                map["credential"] = cred
            }

            val result = CBORWriter().putStringKeyMap(map).compute()

            return Pair(result, null)

        } catch (e: Exception) {

            WAKLogger.w(TAG, "failed to build CBOR: $e - ${e.message}")

            return Pair(null, ErrorReason.Unknown)

        }

    }

}
