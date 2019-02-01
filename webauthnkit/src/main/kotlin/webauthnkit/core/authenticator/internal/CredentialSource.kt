package webauthnkit.core.authenticator.internal

import android.util.Base64
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.CBORReader
import webauthnkit.core.util.CBORWriter

@ExperimentalUnsignedTypes
class PublicKeyCredentialSource(
    var signCount:  UInt,
    val id:         ByteArray,
    val rpId:       String,
    val userHandle: ByteArray,
    val alg:        Int,
    val otherUI:    String
) {

    companion object {

        val TAG = PublicKeyCredentialSource::class.simpleName

        fun fromBase64(str: String): PublicKeyCredentialSource? {
            WAKLogger.d(TAG, "fromBase64")
            return try {
                val bytes = Base64.decode(str, Base64.URL_SAFE)
                // TODO decryption
                fromCBOR(bytes)
            } catch (e: Exception) {
                WAKLogger.w(TAG, "failed to decode Base64: " + e.localizedMessage)
                null
            }
        }

        fun fromCBOR(bytes: ByteArray): PublicKeyCredentialSource? {
            WAKLogger.d(TAG, "fromCBOR")
            return try {

                val map = CBORReader(bytes).readStringKeyMap()!!

                if (!map.containsKey("signCount")) {
                    WAKLogger.w(TAG, "'signCount' key not found")
                    return null
                }
                val signCount = (map["signCount"] as Long).toUInt()

                if (!map.containsKey("alg")) {
                    WAKLogger.w(TAG, "'alg' key not found")
                    return null
                }
                val alg= (map["alg"] as Long).toInt()

                if (!map.containsKey("id")) {
                    WAKLogger.w(TAG, "'id' key not found")
                    return null
                }
                val credId= map["id"] as ByteArray

                if (!map.containsKey("rpId")) {
                    WAKLogger.w(TAG, "'rpId' key not found")
                    return null
                }
                val rpId= map["rpId"] as String

                if (!map.containsKey("userHandle")) {
                    WAKLogger.w(TAG, "'userHandle' key not found")
                    return null
                }
                val userHandle= map["userHandle"] as ByteArray

                if (!map.containsKey("otherUI")) {
                    WAKLogger.w(TAG, "'otherUI' key not found")
                    return null
                }
                val otherUI= map["otherUI"] as String

                return PublicKeyCredentialSource(
                    signCount  = signCount,
                    id         = credId,
                    rpId       = rpId,
                    userHandle = userHandle,
                    alg        = alg,
                    otherUI    = otherUI
                )

            } catch (e: Exception) {
                WAKLogger.w(TAG, "failed to decode CBOR: " + e.localizedMessage)
                null
            }
        }

    }

    val idHex: String
        get() = ByteArrayUtil.toHex(this.id)

    val keyLabel: String
        get() = this.rpId +  "/" + ByteArrayUtil.toHex(this.userHandle)

    fun toBase64(): String? {
        return try {
            val cbor = toCBOR()
            if (cbor != null) {
                // TODO encryption
                ByteArrayUtil.encodeBase64URL(cbor)
            } else {
                null
            }
        } catch(e: Exception) {
            WAKLogger.w(TAG, "failed to encode Base64: " + e.localizedMessage)
            null
        }
    }

    fun toCBOR(): ByteArray? {
        return try {

            val map = LinkedHashMap<String, Any>()
            map["id"]         = this.id
            map["rpId"]       = this.rpId
            map["userHandle"] = this.userHandle
            map["alg"]        = this.alg.toLong()
            map["signCount"]  = this.signCount.toLong()
            map["otherUI"]    = this.otherUI

            return CBORWriter().putStringKeyMap(map).compute()

        } catch (e: Exception) {
            WAKLogger.w(TAG, "failed to encode CBOR: " + e.localizedMessage)
            null
        }

    }

}