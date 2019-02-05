package webauthnkit.core.ctap.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import webauthnkit.core.authenticator.internal.InternalAuthenticator
import webauthnkit.core.ctap.CTAPCommandType
import webauthnkit.core.ctap.CTAPStatusCode
import webauthnkit.core.ctap.ble.frame.FrameBuffer
import webauthnkit.core.ctap.ble.peripheral.*
import webauthnkit.core.ctap.ble.peripheral.annotation.*
import webauthnkit.core.util.CBORReader
import webauthnkit.core.util.WAKLogger

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class BLEFIDOService(
    private val context:       Context,
    private val authenticator: InternalAuthenticator
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
            BLECommandType.Cancel    -> { handleBLECancel()        }
            BLECommandType.MSG       -> { handleBLEMSG(data)       }
            BLECommandType.Error     -> { handleBLEError(data)     }
            BLECommandType.KeepAlive -> { handleBLEKeepAlive(data) }
            BLECommandType.Ping      -> { handleBLEPing(data)      }
        }
    }

    private fun handleBLECancel() {
        WAKLogger.d(TAG, "handleBLE: Cancel")
    }

    private fun handleBLEKeepAlive(value: ByteArray) {
        WAKLogger.d(TAG, "handleBLE: KeepAlive")
        WAKLogger.d(TAG, "should be authenticator -> client")
        // TODO better closing step
        // closeByError(InvalidOperation)
        close()
    }

    private fun handleBLEMSG(value: ByteArray) {
        WAKLogger.d(TAG, "handleBLE: MSG")
        if (value.size < 1) {
            // closeByError(NoEnoughSize)
            return
        }
        val command = value[0].toInt()
        when (command) {
            CTAPCommandType.MakeCredential.rawValue -> {
                if (value.size < 2) {
                    // closeByError(NoEnoughSize)
                    return
                }
                handleCTAPMakeCredential(value.sliceArray(0..value.size))
            }
            CTAPCommandType.GetAssertion.rawValue -> {
                if (value.size < 2) {
                    // closeByError(NoEnoughSize)
                    return
                }
                handleCTAPGetAssertion(value.sliceArray(0..value.size))
            }
            CTAPCommandType.GetNextAssertion.rawValue -> {
                handleCTAPGetNextAssertion()
            }
            CTAPCommandType.ClientPIN.rawValue -> {
                // unsupported
                if (value.size < 2) {
                    // closeByError(NoEnoughSize)
                    return
                }
                handleCTAPClientPIN(value.sliceArray(0..value.size))
            }
            CTAPCommandType.GetInfo.rawValue -> {
                handleCTAPGetInfo()
            }
            CTAPCommandType.Reset.rawValue -> {
                handleCTAPReset()
            }
            else -> {
                handleCTAPUnsupportedCommand()
            }
        }
    }

    private fun handleBLEError(value: ByteArray) {
        WAKLogger.d(TAG, "handleBLE: Error")
        WAKLogger.d(TAG, "should be authenticator -> client")
        // TODO closeByError(InvalidOperation)
        close()
    }

    private fun handleBLEPing(value: ByteArray) {
        WAKLogger.d(TAG, "handleBLE: Ping")
    }

    private fun handleCTAPMakeCredential(value: ByteArray) {
        WAKLogger.d(TAG, "handleCTAP: MakeCredential")
        val params = CBORReader(value).readStringKeyMap()
        if (params == null) {
            closeByError(CTAPStatusCode.InvalidCBOR)
            return
        }
    }

    private fun handleCTAPGetAssertion(value: ByteArray) {
        WAKLogger.d(TAG, "handleCTAP: GetAssertion")
        val params = CBORReader(value).readStringKeyMap()
        if (params == null) {
            closeByError(CTAPStatusCode.InvalidCBOR)
            return
        }
    }

    private fun handleCTAPGetInfo() {
        WAKLogger.d(TAG, "handleCTAP: GetInfo")
    }

    private fun handleCTAPClientPIN(value: ByteArray) {
        WAKLogger.d(TAG, "handleCTAP: ClientPIN")
        WAKLogger.d(TAG, "This feature is not supported on this library. Better verification methods are supported on authenticator side.")
        handleCTAPUnsupportedCommand()
    }

    private fun handleCTAPReset() {
        WAKLogger.d(TAG, "handleCTAP: Reset")
    }

    private fun handleCTAPGetNextAssertion() {
        WAKLogger.d(TAG, "handleCTAP: GetNextAssertion")
        WAKLogger.d(TAG, "This feature is not supported on this library. 'Key Selection' is done on authenticator side.")
        handleCTAPUnsupportedCommand()
    }

    private fun handleCTAPUnsupportedCommand() {
        WAKLogger.d(TAG, "handleCTAP: Unsupported Command")
        // TODO closeByError()
    }

    private fun closeByError(error: CTAPStatusCode) {
        // send error packet as 'status' notification
        // delay 10ms
        // close()
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

                GlobalScope.launch {

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