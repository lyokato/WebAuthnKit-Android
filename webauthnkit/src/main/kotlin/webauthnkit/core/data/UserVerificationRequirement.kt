package webauthnkit.core.data

enum class UserVerificationRequirement(
    private val rawValue: String
) {
    Required("required"),
    Preferred("preferred"),
    Discouraged("discouraged");

    override fun toString(): String {
        return rawValue
    }
}

