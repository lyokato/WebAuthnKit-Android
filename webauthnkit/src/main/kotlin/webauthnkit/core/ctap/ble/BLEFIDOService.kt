package webauthnkit.core.ctap.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import webauthnkit.core.ctap.ble.frame.FrameBuffer
import webauthnkit.core.ctap.ble.peripheral.*
import webauthnkit.core.ctap.ble.peripheral.annotation.*
import webauthnkit.core.util.WAKLogger

class BLEFIDOService(
    private val context: Context
) {

    companion object {
        val TAG = BLEFIDOService::class.simpleName
    }

    private val peripheralListener = object: PeripheralListener {

        override fun onAdvertiseSuccess(settingsInEffect: AdvertiseSettings) {
            WAKLogger.d(TAG, "onAdvertiseSuccess")
        }

        override fun onAdvertiseFailure(errorCode: Int) {
            WAKLogger.d(TAG, "onAdvertiseFailure: $errorCode")
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            WAKLogger.d(TAG, "onConnectionStateChange")
        }
    }

    private fun handleCommand(command: BLECommandType, data: ByteArray) {

        when (command) {
            BLECommandType.Cancel    -> { handleCancel()        }
            BLECommandType.MSG       -> { handleMSG(data)       }
            BLECommandType.Error     -> { handleError(data)     }
            BLECommandType.KeepAlive -> { handleKeepAlive(data) }
            BLECommandType.Ping      -> { handlePing(data)      }
        }
    }

    private fun handleCancel() {
        WAKLogger.d(TAG, "handleCancel")
    }

    private fun handleKeepAlive(value: ByteArray) {
        WAKLogger.d(TAG, "handleKeepAlive")
        WAKLogger.d(TAG, "should be authenticator -> client")
        // TODO better closing step
        close()
    }

    private fun handleMSG(value: ByteArray) {
        WAKLogger.d(TAG, "handleMSG")

    }

    private fun handleError(value: ByteArray) {
        WAKLogger.d(TAG, "handleError")
        WAKLogger.d(TAG, "should be authenticator -> client")
        // TODO better closing step
        close()
    }

    private fun handlePing(value: ByteArray) {
        WAKLogger.d(TAG, "handlePing")
    }

    private fun close() {

    }

    private var frameBuffer = FrameBuffer()

    private var peripheral: Peripheral? = null

    private fun createPeripheral(): Peripheral {

        val service = object: PeripheralService("0xFFFD") {

            @OnWrite("F1D0FFF1-DEAA-ECEE-B42F-C9BA7ED623BB")
            @ResponseNeeded(true)
            @Secure(true)
            fun controlPoint(req: WriteRequest, res: WriteResponse) {
                WAKLogger.d(TAG, "@Write: controlPoint")

                if (frameBuffer.putFragment(req.value)) {

                    if (frameBuffer.isDone()) {

                        handleCommand(frameBuffer.getCommand(), frameBuffer.getData())
                        frameBuffer.clear()
                    }

                } else {
                    frameBuffer.clear()
                    // TODO more detailed error should be passed to 'close'
                    close()
                }
            }

            @OnRead("F1D0FFF1-DEAA-ECEE-B42F-C9BA7ED623BB")
            @Notifiable(true)
            @Secure(true)
            fun status(req: ReadRequest, res: ReadResponse) {
                WAKLogger.d(TAG, "@Read: status")
                WAKLogger.d(TAG, "do nothing")
            }

            @OnRead("F1D0FFF3-DEAA-ECEE-B42F-C9BA7ED623BB")
            @Secure(true)
            fun controlPointLength(req: ReadRequest, res: ReadResponse) {
                WAKLogger.d(TAG, "@Read: controlPointLength")
                // TODO obtain MTU
            }

            @OnWrite("F1D0FFF4-DEAA-ECEE-B42F-C9BA7ED623BB")
            @ResponseNeeded(true)
            @Secure(true)
            fun serviceRevisionBitFieldWrite(req: WriteRequest, res: WriteResponse) {
                WAKLogger.d(TAG, "@Write: serviceRevisionBitField")
                if (req.value.size == 1 && req.value[0].toInt() == 0x20) {
                    res.status = BluetoothGatt.GATT_SUCCESS
                } else {
                    WAKLogger.d(TAG, "@unsupported bitfield")
                    res.status = BluetoothGatt.GATT_FAILURE
                }
            }

            @OnRead("F1D0FFF4-DEAA-ECEE-B42F-C9BA7ED623BB")
            @Secure(true)
            fun serviceRevisionBitFieldRead(req: ReadRequest, res: ReadResponse) {
                WAKLogger.d(TAG, "@Read: serviceRevisionBitField")
                /*
                 Support Version Bitfield

                 Bit 7: U2F 1.1
                 Bit 6: U2F 1.2
                 Bit 5: FIDO2
                 Bit 4: Reserved
                 Bit 3: Reserved
                 Bit 2: Reserved
                 Bit 1: Reserved
                 Bit 0: Reserved

                 This library support only FIDO2, so the bitfield is 0b0010_0000 (0x20)
                 */
                res.write(byteArrayOf(0x20.toByte()))
            }

        }

        return Peripheral(context, service, peripheralListener)
    }

}