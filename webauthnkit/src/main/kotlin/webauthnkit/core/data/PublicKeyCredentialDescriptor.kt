package webauthnkit.core.data

data class PublicKeyCredentialDescriptor(
    val type: PublicKeyCredentialType = PublicKeyCredentialType.PublicKey,
    var id: ByteArray,
    var transports: MutableList<AuthenticatorTransport> = ArrayList()
) {

    fun addTransport(transport: AuthenticatorTransport) {
        this.transports.add(transport)
    }
}

