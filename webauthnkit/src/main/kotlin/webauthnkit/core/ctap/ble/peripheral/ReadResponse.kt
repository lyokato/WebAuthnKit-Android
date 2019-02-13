package webauthnkit.core.ctap.ble.peripheral

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattServer
import webauthnkit.core.util.WAKLogger

class ReadResponse(
    private val req: ReadRequest
) {

    companion object {
        val TAG = ReadResponse::class.simpleName
    }

    var status = BluetoothGatt.GATT_SUCCESS

    fun write(value: ByteArray) {
        req.characteristic.value = value
    }

    internal fun finishOn(server: BluetoothGattServer) {
        WAKLogger.d(TAG, "finishOn")
        if (status == BluetoothGatt.GATT_FAILURE) {
            WAKLogger.d(TAG, "sendResponse(failure)")
            server.sendResponse(req.device, req.requestId, status, 0, null)
        } else {
            WAKLogger.d(TAG, "sendResponse")
            server.sendResponse(req.device, req.requestId, status, 0, req.characteristic.value)
        }
    }

}