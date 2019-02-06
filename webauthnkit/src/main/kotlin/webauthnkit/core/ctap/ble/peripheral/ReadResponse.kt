package webauthnkit.core.ctap.ble.peripheral

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattServer

class ReadResponse(
    private val req: ReadRequest
) {

    var status = BluetoothGatt.GATT_SUCCESS

    fun write(value: ByteArray) {
        req.characteristic.value = value
    }

    internal fun finishOn(server: BluetoothGattServer) {
        if (status == BluetoothGatt.GATT_FAILURE) {
            server.sendResponse(req.device, req.requestId, status, 0, null)
        } else {
            server.sendResponse(req.device, req.requestId, status, 0, req.characteristic.value)
        }
    }

}