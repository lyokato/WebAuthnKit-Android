package webauthnkit.core.ctap.ble

import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.WAKLogger
import java.util.*

class BLEFrameFragmentSplitter(
    maxPacketDataSize: Int // should be (ATT_MTU - 3) ?
) {
    private val firstFragmentMaxDataSize = maxPacketDataSize - 3
    private val restFragmentsMaxDataSize = maxPacketDataSize - 1

    fun splitRequest(
        command: BLECommandType,
        data: ByteArray
    ): Pair<BLERequestFrame, List<BLEContinuationFrame>> {

        val len = data.size

        if (len > firstFragmentMaxDataSize) {

            val firstData = Arrays.copyOfRange(data, 0, firstFragmentMaxDataSize)
            var pos = firstFragmentMaxDataSize
            val first = BLERequestFrame(command, len, firstData)

            val fragments =
                mutableListOf<BLEContinuationFrame>()

            var seq = 0

            while (pos < len) {

                val restSize = len - pos

                if (restSize < restFragmentsMaxDataSize) {

                    val fragment = Arrays.copyOfRange(data, pos, pos + restSize)
                    fragments.add(BLEContinuationFrame(seq, fragment))
                    break

                } else {

                    val fragment = Arrays.copyOfRange(data, pos, pos + restFragmentsMaxDataSize)
                    pos += restFragmentsMaxDataSize
                    fragments.add(BLEContinuationFrame(seq, fragment))
                    seq += 1

                }

            }

            return Pair(first, fragments)

        } else {

            return Pair(BLERequestFrame(command, len, data), listOf())

        }
    }

    fun splitResponse(
        status: BLEStatus,
        data:   ByteArray
    ): Pair<BLEResponseFrame, List<BLEContinuationFrame>> {

        val len = data.size

        if (len > firstFragmentMaxDataSize) {

            val firstData = Arrays.copyOfRange(data, 0, firstFragmentMaxDataSize)
            var pos = firstFragmentMaxDataSize
            val first = BLEResponseFrame(status, len, firstData)

            val fragments =
                mutableListOf<BLEContinuationFrame>()

            var seq = 0

            while (pos < len) {

                val restSize = len - pos

                if (restSize < restFragmentsMaxDataSize) {

                    val fragment = Arrays.copyOfRange(data, pos, pos + restSize)
                    fragments.add(BLEContinuationFrame(seq, fragment))
                    break

                } else {

                    val fragment = Arrays.copyOfRange(data, pos, pos + restFragmentsMaxDataSize)
                    pos += restFragmentsMaxDataSize
                    fragments.add(BLEContinuationFrame(seq, fragment))
                    seq += 1

                }

            }

            return Pair(first, fragments)

        } else {

            return Pair(BLEResponseFrame(status, len, data), listOf())

        }
    }

}

class BLERequestFrame(
    val command: BLECommandType,
    val len:     Int,
    val data:    ByteArray
) {

    companion object {

        val TAG = BLERequestFrame::class.simpleName

        fun fromByteArray(bytes: ByteArray): BLERequestFrame? {

            val size = bytes.size
            if (size < 3) {
                WAKLogger.w(TAG, "invalid BLE frame: no enough size for header")
                return null
            }

            val command = BLECommandType.fromByte(bytes[0])
            if (command == null) {
                WAKLogger.w(TAG, "invalid BLE frame: unknown command type")
                return null
            }

            val len = (bytes[1].toInt() and 0x0000_ff00) or (bytes[2].toInt() and 0x0000_00ff)
            val data = Arrays.copyOfRange(bytes, 3, size)

            return BLERequestFrame(command, len, data)
        }
    }

    fun toByteArray(): ByteArray {
        val hLen: Byte = (len and 0x0000_ff00).shr(8).toByte()
        val lLen: Byte = (len and 0x0000_00ff).toByte()
        val header = byteArrayOf(command.toByte(), hLen, lLen)
        return ByteArrayUtil.merge(header, data)
    }
}

class BLEResponseFrame(
    val status: BLEStatus,
    val len:    Int,
    val data:   ByteArray
) {
    companion object {

        val TAG = BLEResponseFrame::class.simpleName

        fun fromByteArray(bytes: ByteArray): BLEResponseFrame? {

            val size = bytes.size
            if (size < 3) {
                WAKLogger.w(TAG, "invalid BLE frame: no enough size for header")
                return null
            }

            val status = BLEStatus.fromByte(bytes[0])
            if (status == null) {
                WAKLogger.w(TAG, "invalid BLE frame: unknown status type")
                return null
            }

            val len = (bytes[1].toInt() and 0x0000_ff00) or (bytes[2].toInt() and 0x0000_00ff)
            val data = Arrays.copyOfRange(bytes, 3, size)
            return BLEResponseFrame(status, len, data)
        }
    }

    fun toByteArray(): ByteArray {
        val hLen: Byte = (len and 0x0000_ff00).shr(8).toByte()
        val lLen: Byte = (len and 0x0000_00ff).toByte()
        val header = byteArrayOf(status.toByte(), hLen, lLen)
        return ByteArrayUtil.merge(header, data)
    }
}

class BLEContinuationFrame(
    val seq:  Int,
    val data: ByteArray
) {

    companion object {
        val TAG = BLEContinuationFrame::class.simpleName

        fun fromByteArray(bytes: ByteArray): BLEContinuationFrame? {
            val size = bytes.size
            if (size < 2) {
                WAKLogger.w(TAG, "invalid BLE frame: no enough size")
                return null
            }
            val seq = bytes[0].toInt()
            val data = Arrays.copyOfRange(bytes, 1, size)
            return BLEContinuationFrame(seq, data)
        }
    }

    fun toByteArray(): ByteArray {
        return ByteArrayUtil.merge(byteArrayOf(seq.toByte()), data)
    }

}
