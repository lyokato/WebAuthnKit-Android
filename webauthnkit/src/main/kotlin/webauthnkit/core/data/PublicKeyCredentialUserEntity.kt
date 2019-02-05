package webauthnkit.core.data

data class PublicKeyCredentialUserEntity(
    var id: String? = null,
    var name: String = "",
    var displayName: String = "",
    var icon: String? = null
)

