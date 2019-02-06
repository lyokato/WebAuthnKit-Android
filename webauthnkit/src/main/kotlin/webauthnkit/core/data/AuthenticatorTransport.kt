package webauthnkit.core.data

enum class AuthenticatorTransport(
    private val rawValue: String
) {
    USB("usb"),
    BLE("ble"),
    NFC("nfc"),
    Internal("internal");

    override fun toString(): String {
        return rawValue
    }
}

