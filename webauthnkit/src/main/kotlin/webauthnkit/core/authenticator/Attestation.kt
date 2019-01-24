package webauthnkit.core.authenticator

import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.CBORWriter
import webauthnkit.core.util.WAKLogger

@ExperimentalUnsignedTypes
class AttestationObject(
    val fmt:      String,
    val authData: AuthenticatorData,
    val attStmt:  Map<String, Any>
) {

    companion object {
        val TAG = AttestationObject::class.simpleName
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
        if (this.authData.attestedCredentialData!!.aaguid.any { it != 0x00.toByte() }) {
            return false
        }
        return true
    }

    fun toBytes(): ByteArray? {
        WAKLogger.d(TAG, "toBytes")

        return try {
            val authDataBytes = this.authData.toBytes()
            if (authDataBytes == null) {
                WAKLogger.d(TAG, "failed to build authenticator data")
                return null
            }
            val map = LinkedHashMap<String, Any>()
            map["authData"] = authDataBytes
            map["fmt"]      = this.fmt
            map["attStmt"]  = this.attStmt

            WAKLogger.d(TAG, "AUTH_DATA: " + ByteArrayUtil.toHex(authDataBytes))

            return CBORWriter().putStringKeyMap(map).compute()

        } catch (e: Exception) {
            WAKLogger.d(TAG, "failed to build attestation binary: " + e.localizedMessage)
            null

        }

    }

}
