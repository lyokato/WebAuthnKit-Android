package webauthnkit.core.ctap.options

import webauthnkit.core.ctap.ble.BLEErrorType
import webauthnkit.core.data.PublicKeyCredentialParameters
import webauthnkit.core.data.PublicKeyCredentialRpEntity
import webauthnkit.core.data.PublicKeyCredentialUserEntity
import webauthnkit.core.util.CBORReader
import webauthnkit.core.util.WAKLogger

@ExperimentalUnsignedTypes
class MakeCredentialOptions(
    val rp                      : PublicKeyCredentialRpEntity,
    val user                    : PublicKeyCredentialUserEntity,
    val clientDataHash          : ByteArray,
    val pubKeyCredParams        : List<PublicKeyCredentialParameters>,
    val requireUserVerification : Boolean,
    val requireResidentKey      : Boolean

) {
    companion object {
        val TAG = MakeCredentialOptions::class.simpleName

        fun fromByteArray(value: ByteArray): Pair<MakeCredentialOptions?, BLEErrorType?> {

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

            if (!params.containsKey("rp")) {
                WAKLogger.d(TAG, "missing 'rp'")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            if (params["rp"] !is Map<*,*>) {
                WAKLogger.d(TAG, "'rp' is not a Map")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            val rpMap = params["rp"] as Map<String, Any>

            if (!rpMap.containsKey("id")) {
                WAKLogger.d(TAG, "missing 'rp:id'")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            if (rpMap["id"] !is String) {
                WAKLogger.d(TAG, "'rp:id' is not a String")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            val rpId = rpMap["id"] as String

            if (!rpMap.containsKey("name")) {
                WAKLogger.d(TAG, "missing 'rp:name'")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            if (rpMap["name"] !is String) {
                WAKLogger.d(TAG, "'rp:name' is not a String")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            val rpName = rpMap["name"] as String

            val rp = PublicKeyCredentialRpEntity(
                id   = rpId,
                name = rpName
            )
            if (rpMap.containsKey("icon") && rpMap["icon"] is String) {
                rp.icon = rpMap["icon"] as String
            }

            if (!params.containsKey("user")) {
                WAKLogger.d(TAG, "missing 'user'")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            if (params["user"] !is Map<*,*>) {
                WAKLogger.d(TAG, "'user' is not a Map")
                return Pair(null, BLEErrorType.InvalidPar)
            }

            val userMap = params["user"] as Map<String, Any>

            if (!userMap.containsKey("id")) {
                WAKLogger.d(TAG, "missing 'user:id'")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            if (userMap["id"] !is ByteArray) {
                WAKLogger.d(TAG, "'user:id' is not a ByteArray")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            val userId = userMap["id"] as ByteArray

            if (!userMap.containsKey("name")) {
                WAKLogger.d(TAG, "missing 'user:name'")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            if (userMap["name"] !is String) {
                WAKLogger.d(TAG, "'user:name' is not a String")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            val userName = userMap["name"] as String
            val user = PublicKeyCredentialUserEntity(
                id   = userId,
                name = userName
            )
            if (userMap.containsKey("displayName") && userMap["displayName"] is String) {
                user.displayName = userMap["displayName"] as String
            }
            if (userMap.containsKey("icon") && userMap["icon"] is String) {
                user.icon = userMap["icon"] as String
            }


            if (!params.containsKey("pubKeyCredParams")) {
                WAKLogger.d(TAG, "missing 'pubKeyCredParams'")
                return Pair(null, BLEErrorType.InvalidPar)
            }
            if (params["pubKeyCredParams"] !is List<*>) {
                WAKLogger.d(TAG, "'pubKeyCredParams' is not a List")
                return Pair(null, BLEErrorType.InvalidPar)
            }

            val pubKeyCredParams = mutableListOf<PublicKeyCredentialParameters>()

            (params["pubKeyCredParams"] as List<Any>).forEach {

                if (it !is Map<*, *>) {
                    WAKLogger.d(TAG, "'pubKeyCredParam' is not a Map")
                    return Pair(null, BLEErrorType.InvalidPar)
                }

                val credParam = it as Map<String, Any>

                if (!credParam.containsKey("type")) {
                    WAKLogger.d(TAG, "missing 'type'")
                    return Pair(null, BLEErrorType.InvalidPar)
                }

                if (!credParam.containsKey("alg")) {
                    WAKLogger.d(TAG, "missing 'alg'")
                    return Pair(null, BLEErrorType.InvalidPar)
                }

            }

            // params["excludeList"] optional
            // params["extensions"]

            // This library doesn't support PIN protocol
            // params["pinAuth"] optional
            // params["pinProtocol"] optional

            // This library forces to use RK currently
            val requireResidentKey = true

            var requireUserVerification = false

            if (params.containsKey("options")) {

                if (params["options"] is Map<*,*>) {
                    WAKLogger.d(TAG, "'options' is not a Map")
                    return Pair(null, BLEErrorType.InvalidPar)
                }

                val options = params["options"] as Map<String, Any>

                if (options.containsKey("uv")) {
                    if (options["uv"] is Boolean) {
                        requireUserVerification = options["uv"] as Boolean
                    }
                }

            }

           val options = MakeCredentialOptions(
                rp                      = rp,
                user                    = user,
                clientDataHash          = clientDataHash,
                pubKeyCredParams        = pubKeyCredParams,
                requireUserVerification = requireUserVerification,
                requireResidentKey      = requireResidentKey
            )

            return Pair(options, null)
        }
    }


}