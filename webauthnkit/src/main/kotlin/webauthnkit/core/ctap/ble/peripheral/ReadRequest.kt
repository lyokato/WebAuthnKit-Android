package webauthnkit.core.ctap.ble.peripheral

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic

class ReadRequest(
    val device:         BluetoothDevice,
    val characteristic: BluetoothGattCharacteristic,
    val requestId:      Int,
    val offset:         Int
) {
    val uuid = characteristic.uuid.toString()
}
