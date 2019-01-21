package webauthnkit.core.authenticator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.WAKLogger

@ExperimentalUnsignedTypes
class AttestationObject(
    val fmt:      String,
    val authData: AuthenticatorData,
    val attStmt:  Map<String, Any>
) {

    companion object {
        val TAG = this::class.simpleName
    }

    fun toNone(): AttestationObject {
        return AttestationObject(
            fmt      = "none",
            authData = this.authData,
            attStmt  = HashMap()
        )
    }

    fun isSelfAttestation(): Boolean {
        WAKLogger.d(TAG, "isSelfAttestation")
        if (this.fmt != "packed") {
            return false
        }
        if (this.attStmt.containsKey("x5c")) {
            return false
        }
        if (this.attStmt.containsKey("ecdaaKeyId")) {
            return false
        }
        if (this.authData.attestedCredentialData != null) {
           return false
        }
        if (this.authData.attestedCredentialData!!.aaguid.any { it != 0x00.toUByte() }) {
            return false
        }
        return true
    }

    fun toBytes(): UByteArray? {
        WAKLogger.d(TAG, "toBytes")

        return try {
            val authDataBytes = this.authData.toBytes()
            if (authDataBytes == null) {
                WAKLogger.d(TAG, "failed to build authenticator data")
                return null
            }
            val map = LinkedHashMap<String, Any>()
            map["authData"] = authDataBytes.toByteArray()
            map["fmt"]      = this.fmt
            map["attStmt"]  = this.attStmt

            WAKLogger.d(TAG, "AUTH_DATA: " + ByteArrayUtil.toHex(authDataBytes.toByteArray()))

            ObjectMapper(CBORFactory())
                .writeValueAsBytes(map)
                .toUByteArray()

        } catch (e: Exception) {
            WAKLogger.d(TAG, "failed to build attestation binary: " + e.localizedMessage)
            null

        }

    }

}
