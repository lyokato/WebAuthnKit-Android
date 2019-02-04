package webauthnkit.core.ctap.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import webauthnkit.core.ctap.ble.peripheral.*
import webauthnkit.core.ctap.ble.peripheral.annotation.Notifiable
import webauthnkit.core.ctap.ble.peripheral.annotation.OnRead
import webauthnkit.core.ctap.ble.peripheral.annotation.OnWrite
import webauthnkit.core.ctap.ble.peripheral.annotation.Secure
import webauthnkit.core.util.WAKLogger

class BLEFIDOService(
    private val context: Context
) {

    private val peripheralListener = object: PeripheralListener {

        override fun onAdvertiseSuccess(settingsInEffect: AdvertiseSettings) {

        }

        override fun onAdvertiseFailure(errorCode: Int) {

        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {

        }
    }

    private var peripheral: Peripheral? = null

    private fun createPeripheral(): Peripheral {

        val service = object: PeripheralService("0xFFFD") {

            @OnWrite("F1D0FFF1-DEAA-ECEE-B42F-C9BA7ED623BB")
            @Secure(true)
            fun controlPoint(req: WriteRequest, res: WriteResponse) {
                WAKLogger.d(TAG, "@Write: controlPoint")
            }

            @OnRead("F1D0FFF1-DEAA-ECEE-B42F-C9BA7ED623BB")
            @Notifiable(true)
            @Secure(true)
            fun status(req: ReadRequest, res: ReadResponse) {
                WAKLogger.d(TAG, "@Read: status")
            }

            @OnRead("F1D0FFF3-DEAA-ECEE-B42F-C9BA7ED623BB")
            @Secure(true)
            fun controlPointLength(req: ReadRequest, res: ReadResponse) {
                WAKLogger.d(TAG, "@Read: controlPointLength")
                // MTU
            }

            @OnWrite("F1D0FFF4-DEAA-ECEE-B42F-C9BA7ED623BB")
            @Secure(true)
            fun serviceRevisionBitFieldWrite(req: WriteRequest, res: WriteResponse) {
                WAKLogger.d(TAG, "@Write: serviceRevisionBitField")

            }

            @OnRead("F1D0FFF4-DEAA-ECEE-B42F-C9BA7ED623BB")
            @Secure(true)
            fun serviceRevisionBitFieldRead(req: ReadRequest, res: ReadResponse) {
                WAKLogger.d(TAG, "@Read: serviceRevisionBitField")
                // respond 0x20
            }

            @OnWrite("0x2A28")
            @Secure(true)
            fun serviceRevision() {
                WAKLogger.d(TAG, "@Read: serviceRevision")
            }

        }

        return Peripheral(context, service, peripheralListener)
    }

}