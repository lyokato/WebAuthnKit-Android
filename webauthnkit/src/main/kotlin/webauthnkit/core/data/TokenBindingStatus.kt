package webauthnkit.core.data

enum class TokenBindingStatus(
    private val rawValue: String
) {
    Present("present"),
    Supported("supported");

    override fun toString(): String {
        return rawValue
    }

}

