package webauthnkit.core.ctap.options

import webauthnkit.core.ctap.ble.BLEErrorType
import webauthnkit.core.data.PublicKeyCredentialDescriptor
import webauthnkit.core.util.CBORReader
import webauthnkit.core.util.WAKLogger

@ExperimentalUnsignedTypes
class GetAssertionOptions(
    val rpId                    : String,
    val clientDataHash          : ByteArray,
    val allowCredential         : List<PublicKeyCredentialDescriptor>,
    val requireUserVerification : Boolean,
    val requireUserPresence     : Boolean
) {

    companion object {
        val TAG = GetAssertionOptions::class.simpleName

        fun fromByteArray(value: ByteArray): Pair<GetAssertionOptions?, BLEErrorType?> {

            val params = CBORReader(value).readStringKeyMap()
            if (params == null) {
                WAKLogger.d(TAG, "failed to parse as CBOR")
                return Pair(null, BLEErrorType.InvalidPar)
            }

            if (!params.containsKey("clientDataHash")) {
                WAKLogger.d(TAG, "missing 'clientDataHash'")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            if (params["clientDataHash"] !is ByteArray) {
                WAKLogger.d(TAG, "'clientDataHash' is not a ByteArray")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            val clientDataHash = params["clientDataHash"] as ByteArray

            if (!params.containsKey("rpId")) {
                WAKLogger.d(TAG, "missing 'clientDataHash'")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            if (params["rpId"] !is String) {
                WAKLogger.d(TAG, "'rpId' is not a String")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            val rpId = params["rpId"] as String

            // This library forces to use UP currently
            val requireUserPresence = true

            var requireUserVerification = false

            if (params.containsKey("options")) {

                if (params["options"] !is Map<*,*>) {
                    WAKLogger.d(TAG, "'options' is not a Map")
                    return Pair(null, BLEErrorType.InvalidPar)
                }

                val options = params["options"] as Map<String, Any>

                if (options.containsKey("uv")) {
                    if (options["uv"] !is Boolean) {
                        requireUserVerification = options["uv"] as Boolean
                    }
                }

            }

            val options = GetAssertionOptions(
                rpId                    = rpId,
                clientDataHash          = clientDataHash,
                allowCredential         = arrayListOf(),
                requireUserVerification = requireUserVerification,
                requireUserPresence     = requireUserPresence
            )

            return Pair(options, null)
        }

    }

}