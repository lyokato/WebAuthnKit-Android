package webauthnkit.core.data

data class PublicKeyCredential<T: AuthenticatorResponse>(
    val type: PublicKeyCredentialType = PublicKeyCredentialType.PublicKey,
    var id: String,
    var rawId: ByteArray,
    var response: T
)

