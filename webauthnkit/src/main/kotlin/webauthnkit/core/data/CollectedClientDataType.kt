package webauthnkit.core.data

enum class CollectedClientDataType(
    private val rawValue: String
) {
    Create("webauthn.create"),
    Get("webauthn.get");

    override fun toString(): String {
        return rawValue
    }
}

