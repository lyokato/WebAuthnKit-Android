package webauthnkit.core.ctap.ble.frame

import webauthnkit.core.ctap.ble.BLECommandType
import webauthnkit.core.ctap.ble.BLEErrorType
import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.WAKLogger
import java.util.*

class Frame(
    val command: BLECommandType,
    val len:     Int,
    val data:    ByteArray
) {

    companion object {

        val TAG = Frame::class.simpleName

        fun fromByteArray(bytes: ByteArray): Pair<Frame?, BLEErrorType?> {

            val size = bytes.size
            if (size < 3) {
                WAKLogger.w(TAG, "invalid BLE frame: no enough size for header")
                return Pair(null, BLEErrorType.InvalidLen)
            }

            val firstByte = bytes[0]
            val command = BLECommandType.fromByte(firstByte)
            if (command == null) {
                WAKLogger.w(TAG, "invalid BLE frame: unknown command type")
                return Pair(null, BLEErrorType.InvalidCmd)
            }

            val len = (bytes[1].toInt() and 0x0000_ff00) or (bytes[2].toInt() and 0x0000_00ff)
            val data = Arrays.copyOfRange(bytes, 3, size)

            return Pair(Frame(command, len, data), null)
        }
    }

    fun toByteArray(): ByteArray {
        val hLen: Byte = (len and 0x0000_ff00).shr(8).toByte()
        val lLen: Byte = (len and 0x0000_00ff).toByte()
        val header = byteArrayOf(command.toByte(), hLen, lLen)
        return ByteArrayUtil.merge(header, data)
    }
}
