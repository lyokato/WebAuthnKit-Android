package webauthnkit.core.ctap.ble.frame

import webauthnkit.core.ctap.ble.BLECommandType
import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.WAKLogger

class FrameBuffer {

    companion object {
        val TAG = FrameBuffer::class.simpleName
    }

    private var waitingContinuation = false
    private var done = false
    private var seq = -1
    private var command: BLECommandType? = null
    private var len: Int = 0
    private var data: ByteArray = byteArrayOf()

    fun clear() {
        waitingContinuation = false
        done = false
        seq = -1
        command = null
        len = 0
        data = byteArrayOf()
    }

    fun putFragment(value: ByteArray): Boolean {

        if (done) {
            WAKLogger.d(TAG, "always got enough data. process and discard buffer.")
            return false
        }

        if (waitingContinuation) {

            val contFrame = ContinuationFrame.fromByteArray(value)
            if (contFrame == null) {
                WAKLogger.w(TAG, "failed to obtain continuation frame, discard this buffer")
                return false
            }

            if (seq < contFrame.seq) {

                data = ByteArrayUtil.merge(data, contFrame.data)

                if (len <= data.size) {
                    done = true
                } else {
                    seq = contFrame.seq
                }

            } else {
                WAKLogger.d(TAG, "invalid seq in continuation frame, discard this buffer")
                return false
            }

        } else {

            val initFrame = Frame.fromByteArray(value)
            if (initFrame == null) {
                WAKLogger.d(TAG, "failed to obtain init frame, discard this buffer")
                return false
            }

            command = initFrame.command
            len     = initFrame.len
            data    = initFrame.data

            if (len <= data.size) {
                done = true
            } else {
                waitingContinuation = true
            }
        }

        return true
    }

    fun isDone(): Boolean {
        return done
    }

    fun getCommand(): BLECommandType {
       return command!!
    }

    fun getData(): ByteArray {
        return data
    }

}