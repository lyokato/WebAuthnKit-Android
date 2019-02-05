package webauthnkit.core.data

open class AuthenticatorResponse

data class AuthenticatorAttestationResponse(
    var clientDataJSON:    String,
    var attestationObject: ByteArray
): AuthenticatorResponse()

data class AuthenticatorAssertionResponse(
    var clientDataJSON:    String,
    var authenticatorData: ByteArray,
    var signature:         ByteArray,
    var userHandle:        ByteArray?
): AuthenticatorResponse()

