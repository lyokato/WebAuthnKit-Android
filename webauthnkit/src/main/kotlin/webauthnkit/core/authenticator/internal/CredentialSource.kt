package webauthnkit.core.authenticator.internal

import android.util.Base64
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import webauthnkit.core.util.WKLogger
import webauthnkit.core.util.ByteArrayUtil

@ExperimentalUnsignedTypes
class PublicKeyCredentialSource(
    var signCount:  Int,
    val id:         ByteArray,
    val rpId:       String,
    val userHandle: ByteArray,
    val alg:        Int,
    val otherUI:    String
) {

    companion object {

        val TAG = this::class.simpleName

        fun fromBase64(str: String): PublicKeyCredentialSource? {
            WKLogger.d(TAG, "fromBase64")
            return try {
                val bytes = Base64.decode(str, Base64.URL_SAFE)
                // TODO decryption
                fromCBOR(bytes)
            } catch (e: Exception) {
                WKLogger.w(TAG, "failed to decode Base64: " + e.localizedMessage)
                null
            }
        }

        fun fromCBOR(bytes: ByteArray): PublicKeyCredentialSource? {
            WKLogger.d(TAG, "fromCBOR")
            return try {
                ObjectMapper(CBORFactory()).readValue(bytes,
                    PublicKeyCredentialSource::class.java)
            } catch (e: Exception) {
                WKLogger.w(TAG, "failed to decode CBOR: " + e.localizedMessage)
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
            WKLogger.w(TAG, "failed to encode Base64: " + e.localizedMessage)
            null
        }
    }

    fun toCBOR(): UByteArray? {
        return try {
            val map = LinkedHashMap<String, Any>()
            map["id"]         = this.id
            map["rpId"]       = this.rpId
            map["userHandle"] = this.userHandle
            map["alg"]        = this.alg
            map["signCount"]  = this.signCount
            map["otherUI"]    = this.otherUI
            ObjectMapper(CBORFactory())
                .writeValueAsBytes(map)
                .toUByteArray()
        } catch (e: Exception) {
            WKLogger.w(TAG, "failed to encode CBOR: " + e.localizedMessage)
            null
        }

    }

}