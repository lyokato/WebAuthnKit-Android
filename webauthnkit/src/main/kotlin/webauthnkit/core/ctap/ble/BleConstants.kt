package webauthnkit.core.ctap.ble

enum class BLEErrorType(val rawValue: Byte) {
    InvalidCmd(0x01.toByte()),
    InvalidPar(0x02.toByte()),
    InvalidLen(0x03.toByte()),
    InvalidSeq(0x04.toByte()),
    ReqTimeout(0x05.toByte()),
    Other(0x7f.toByte());

    fun toByte(): Byte {
        return rawValue
    }
}

enum class BLECommandType(val rawValue: Byte) {

    Ping(0x81.toByte()),
    KeepAlive(0x82.toByte()),
    MSG(0x83.toByte()),
    Cancel(0xBE.toByte()),
    Error(0xBF.toByte());

    companion object {
        fun fromByte(byte: Byte): BLECommandType? {
            return when (byte) {
                0x81.toByte() -> Ping
                0x82.toByte() -> KeepAlive
                0x83.toByte() -> MSG
                0xBE.toByte() -> Cancel
                0xBF.toByte() -> Error
                else -> null
            }
        }
    }

    fun toByte(): Byte {
        return rawValue
    }

}

enum class BLEKeepAliveStatus(val rawValue: Int) {

    Processing(0x01),
    UPNeeded(0x02),
    RFU(0x00);

    companion object {
        fun fromByte(byte: Byte): BLEKeepAliveStatus? {
            return when (byte.toInt()) {
                0x01 -> Processing
                0x02 -> UPNeeded
                0x00 -> RFU
                else -> null
            }
        }
    }

    fun toByte(): Byte {
        return rawValue.toByte()
    }

}

