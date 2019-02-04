package webauthnkit.core.ctap.ble.frame

import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.WAKLogger
import java.util.*

class ContinuationFrame(
    val seq:  Int,
    val data: ByteArray
) {

    companion object {
        val TAG = ContinuationFrame::class.simpleName

        fun fromByteArray(bytes: ByteArray): ContinuationFrame? {
            val size = bytes.size
            if (size < 2) {
                WAKLogger.w(TAG, "invalid BLE frame: no enough size")
                return null
            }
            val seq = bytes[0].toInt()
            val data = Arrays.copyOfRange(bytes, 1, size)
            return ContinuationFrame(seq, data)
        }
    }

    fun toByteArray(): ByteArray {
        return ByteArrayUtil.merge(byteArrayOf(seq.toByte()), data)
    }

}
