package webauthnkit.core.ctap.ble.peripheral

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic

class WriteRequest(
    val device:         BluetoothDevice,
    val characteristic: BluetoothGattCharacteristic,
    val requestId:      Int,
    val offset:         Int,
    val preparedWrite:  Boolean,
    val responseNeeded: Boolean,
    val value:          ByteArray
) {
    val uuid = characteristic.uuid.toString()
}
