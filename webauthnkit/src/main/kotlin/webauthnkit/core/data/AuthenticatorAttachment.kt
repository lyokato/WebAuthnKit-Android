package webauthnkit.core.data

enum class AuthenticatorAttachment(
    private val rawValue: String
) {
    Platform("platform"),
    CrossPlatform("cross-platform");

    override fun toString(): String {
        return rawValue
    }
}

