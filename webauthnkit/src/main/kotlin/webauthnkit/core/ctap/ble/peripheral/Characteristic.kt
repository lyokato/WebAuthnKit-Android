package webauthnkit.core.ctap.ble.peripheral

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import webauthnkit.core.ctap.ble.BleEvent
import webauthnkit.core.util.WAKLogger
import java.lang.reflect.Method
import java.util.*

class Characteristic(
    val uuid: String
) {
    companion object {
        val TAG = Characteristic::class.simpleName
        const val CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    }

    private val handlers: MutableMap<BleEvent, Method> = mutableMapOf()
    private val devicesForNotification: MutableMap<String, BluetoothDevice> = mutableMapOf()
    private var properties: Int = 0
    private var permission: Int = 0

    fun getDevicesToNotify(): Collection<BluetoothDevice> {
        return devicesForNotification.values
    }

    fun rememberDeviceForNotification(device: BluetoothDevice) {
        devicesForNotification[device.address] = device
    }

    fun forgetDeviceForNotification(device: BluetoothDevice) {
        devicesForNotification.remove(device.address)
    }

    fun addPermission(perm: Int) {
        permission = permission or perm
    }

    fun addProperty(prop: Int) {
        properties = properties or prop
    }

    fun canHandle(event: BleEvent): Boolean {
        return handlers.containsKey(event)
    }

    fun addHandler(event: BleEvent, handler: Method) {
        handlers[event] = handler
    }

    fun handleReadRequest(parent: PeripheralService, req: ReadRequest, res: ReadResponse) {
        if (canHandle(BleEvent.READ)) {
            val method = handlers.getValue(BleEvent.READ)
            try {
                method.invoke(parent, req, res)
            } catch (e: Exception) {
                WAKLogger.w(TAG, "Exception happened during read request: $e")
            }
        }
    }

    fun handleWriteRequest(parent: PeripheralService, req: WriteRequest, res: WriteResponse) {
        if (canHandle(BleEvent.WRITE)) {
            val method = handlers.getValue(BleEvent.WRITE)
            try {
                method.invoke(parent, req, res)
            } catch (e: Exception) {
                WAKLogger.w(TAG, "Exception happened during write request: $e")
            }

        }
    }

    internal fun createRaw(): BluetoothGattCharacteristic {
        val ch = BluetoothGattCharacteristic(UUID.fromString(uuid), properties, permission)
        if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
            // TODO need PERMISSION_READ_ENCRYPT?
            val descriptor = BluetoothGattDescriptor(
                UUID.fromString(CONFIG_UUID),
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            ch.addDescriptor(descriptor)
        }
        return ch
    }

}