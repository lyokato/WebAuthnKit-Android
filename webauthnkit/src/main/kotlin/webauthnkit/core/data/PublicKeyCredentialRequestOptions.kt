package webauthnkit.core.data

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

