package webauthnkit.core.ctap.ble.frame

import webauthnkit.core.ctap.ble.BLECommandType
import webauthnkit.core.ctap.ble.BLEErrorType
import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.WAKLogger

class FrameBuffer {

    companion object {
        val TAG = FrameBuffer::class.simpleName
    }

    private var waitingContinuation: Boolean = false
    private var done:                Boolean = false
    private var command:             BLECommandType? = null
    private var data:                ByteArray = byteArrayOf()
    private var seq:                 Int = -1
    private var len:                 Int = 0

    fun clear() {
        waitingContinuation = false
        done                = false
        seq                 = -1
        command             = null
        len                 = 0
        data                = byteArrayOf()
    }

    fun putFragment(value: ByteArray): BLEErrorType? {

        WAKLogger.d(TAG, "putFragment: ${value.size} bytes")
        if (done) {
            WAKLogger.d(TAG, "always got enough data. process and discard buffer.")
            return BLEErrorType.InvalidLen
        }

        if (waitingContinuation) {

            WAKLogger.d(TAG, "waiting continuation, put as rest")

            val (contFrame, err) =
                ContinuationFrame.fromByteArray(value)
            if (contFrame == null) {
                WAKLogger.w(TAG, "failed to obtain continuation frame, discard this buffer")
                return err
            }

            if (seq < contFrame.seq) {

                data = ByteArrayUtil.merge(data, contFrame.data)

                if (len <= data.size) {
                    WAKLogger.d(TAG, "done")
                    done = true
                } else {
                    seq = contFrame.seq
                }

            } else {
                WAKLogger.d(TAG, "invalid seq in continuation frame, discard this buffer")
                return BLEErrorType.InvalidLen
            }

        } else {

            WAKLogger.d(TAG, "init frame")

            val (initFrame, err) = Frame.fromByteArray(value)
            if (initFrame == null) {
                WAKLogger.d(TAG, "failed to obtain init frame, discard this buffer")
                return err
            }

            command = initFrame.command
            len     = initFrame.len
            data    = initFrame.data

            if (len <= data.size) {
                WAKLogger.d(TAG, "done")
                done = true
            } else {
                waitingContinuation = true
            }
        }

        return null
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