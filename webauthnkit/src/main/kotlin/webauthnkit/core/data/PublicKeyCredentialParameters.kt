package webauthnkit.core.data

data class PublicKeyCredentialParameters(
    val type: PublicKeyCredentialType = PublicKeyCredentialType.PublicKey,
    var alg: Int
)

