package webauthnkit.core.data

enum class AttestationConveyancePreference(
    private val rawValue: String
) {

    None("none"),
    Direct("direct"),
    Indirect("indirect");

    override fun toString(): String {
        return rawValue
    }
}

