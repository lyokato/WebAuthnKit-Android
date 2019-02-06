package webauthnkit.core.data

data class CollectedClientData(
    //val type: CollectedClientDataType,
    val type: String,
    var challenge: String,
    var origin: String,
    var tokenBinding: TokenBinding? = null
)

