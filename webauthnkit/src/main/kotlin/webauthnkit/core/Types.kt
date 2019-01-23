package webauthnkit.core

import java.lang.RuntimeException

class BadOperationException(msg: String? = null) : RuntimeException(msg)
class InvalidStateException(msg: String? = null) : RuntimeException(msg)
class ConstraintException(msg: String? = null) : RuntimeException(msg)
class CancelledException(msg: String? = null) : RuntimeException(msg)
class TimeoutException(msg: String? = null) : RuntimeException(msg)
class NotAllowedException(msg: String? = null) : RuntimeException(msg)
class UnsupportedException(msg: String? = null) : RuntimeException(msg)
class UnknownException(msg: String? = null) : RuntimeException(msg)

enum class ErrorReason(val rawValue: RuntimeException) {
    BadOperation(BadOperationException()),
    InvalidState(InvalidStateException()),
    Constraint(ConstraintException()),
    Cancelled(CancelledException()),
    Timeout(TimeoutException()),
    NotAllowed(NotAllowedException()),
    Unsupported(UnsupportedException()),
    Unknown(UnknownException())

}

enum class PublicKeyCredentialType(val rawValue: String) {
    PublicKey("public-key");

    override fun toString(): String {
        return rawValue
    }
}

enum class UserVerificationRequirement(val rawValue: String) {
    Required("required"),
    Preferred("preferred"),
    Discouraged("discouraged");

    override fun toString(): String {
        return rawValue
    }
}

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

data class PublicKeyCredential<T: AuthenticatorResponse>(
    val type: PublicKeyCredentialType = PublicKeyCredentialType.PublicKey,
    var id: String,
    var rawId: ByteArray,
    var response: T
)

enum class AuthenticatorTransport(val rawValue: String) {
    USB("usb"),
    BLE("ble"),
    NFC("nfc"),
    Internal("internal");

    override fun toString(): String {
        return rawValue
    }
}

data class PublicKeyCredentialDescriptor(
    val type: PublicKeyCredentialType = PublicKeyCredentialType.PublicKey,
    var id: ByteArray,
    var transports: MutableList<AuthenticatorTransport> = ArrayList()
) {

    fun addTransport(transport: AuthenticatorTransport) {
        this.transports.add(transport)
    }
}

data class PublicKeyCredentialRpEntity(
    var id: String? = null,
    var name: String = "",
    var icon: String? = null
)

data class PublicKeyCredentialUserEntity(
    var id: String? = null,
    var name: String = "",
    var displayName: String = "",
    var icon: String? = null
)

enum class AttestationConveyancePreference(val rawValue: String) {

    None("none"),
    Direct("direct"),
    Indirect("indirect");

    override fun toString(): String {
        return rawValue
    }
}

data class PublicKeyCredentialParameters(
    val type: PublicKeyCredentialType = PublicKeyCredentialType.PublicKey,
    var alg: Int
)

enum class TokenBindingStatus(val rawValue: String) {
    Present("present"),
    Supported("supported");

    override fun toString(): String {
        return rawValue
    }

}

data class TokenBinding(
    var status: TokenBindingStatus,
    var id: String
)

enum class CollectedClientDataType(val rawValue: String) {
    Create("webauthn.create"),
    Get("webauthn.get");

    override fun toString(): String {
        return rawValue
    }
}

data class CollectedClientData(
    //val type: CollectedClientDataType,
    val type: String,
    var challenge: String,
    var origin: String,
    var tokenBinding: TokenBinding? = null
)

enum class AuthenticatorAttachment(val rawValue: String) {
    Platform("platform"),
    CrossPlatform("cross-platform");

    override fun toString(): String {
        return rawValue
    }
}

data class AuthenticatorSelectionCriteria(
    var authenticatorAttachment: AuthenticatorAttachment? = null,
    var requireResidentKey: Boolean = true,
    var userVerification: UserVerificationRequirement = UserVerificationRequirement.Required
)

class PublicKeyCredentialCreationOptions(
    var rp: PublicKeyCredentialRpEntity = PublicKeyCredentialRpEntity(),
    var user: PublicKeyCredentialUserEntity = PublicKeyCredentialUserEntity(),
    var challenge: ByteArray = ByteArray(0),
    var pubKeyCredParams: MutableList<PublicKeyCredentialParameters> = ArrayList(),
    var timeout: Long? = null,
    var excludeCredentials: MutableList<PublicKeyCredentialDescriptor> = ArrayList(),
    var authenticatorSelection: AuthenticatorSelectionCriteria? = null,
    var attestation: AttestationConveyancePreference = AttestationConveyancePreference.Direct,
    var extensions: Map<String, Any> = HashMap()
) {
    fun addPubKeyCredParam(alg: Int) {
        this.pubKeyCredParams.add(PublicKeyCredentialParameters(alg = alg))
    }
}

data class PublicKeyCredentialRequestOptions(
    var challenge: ByteArray = ByteArray(0),
    var rpId: String? = null,
    var allowCredential: MutableList<PublicKeyCredentialDescriptor> = ArrayList(),
    var userVerification: UserVerificationRequirement = UserVerificationRequirement.Required,
    var timeout: Long? = null
) {
    fun addAllowCredential(credentialId: ByteArray, transports: MutableList<AuthenticatorTransport>) {
        this.allowCredential.add(
            PublicKeyCredentialDescriptor(
                id         = credentialId,
                transports = transports
            )
        )
    }
}

typealias MakeCredentialResponse = PublicKeyCredential<AuthenticatorAttestationResponse>

typealias GetAssertionResponse = PublicKeyCredential<AuthenticatorAssertionResponse>
