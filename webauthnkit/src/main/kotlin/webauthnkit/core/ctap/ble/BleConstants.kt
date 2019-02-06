package webauthnkit.core.ctap.ble

enum class BLEErrorType(val rawValue: Int) {
    InvalidCmd(0x01),
    InvalidPar(0x02),
    InvalidLen(0x03),
    InvalidSeq(0x04),
    ReqTimeout(0x05),
    Other(0x7f);

    fun toByte(): Byte {
        return rawValue.toByte()
    }
}

enum class BLECommandType(val rawValue: Int) {

    Ping(0x81),
    KeepAlive(0x82),
    MSG(0x83),
    Cancel(0xBE),
    Error(0xBF);

    companion object {
        fun fromByte(byte: Byte): BLECommandType? {
            return when (byte.toInt()) {
                0x81 -> Ping
                0x82 -> KeepAlive
                0x83 -> MSG
                0xBE -> Cancel
                0xBF -> Error
                else -> null
            }
        }
    }

    fun toByte(): Byte {
        return rawValue.toByte()
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

enum class BLEError(val rawValue: Int) {

    InvalidCMD(0x01),
    InvalidPar(0x02),
    InvalidLen(0x03),
    InvalidSeq(0x04),
    ReqTimeout(0x05),
    Other(0x7f);

    fun toByte(): Byte {
        return rawValue.toByte()
    }

}