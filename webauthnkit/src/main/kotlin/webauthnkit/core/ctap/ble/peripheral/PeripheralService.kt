package webauthnkit.core.ctap.ble.peripheral

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.os.ParcelUuid
import webauthnkit.core.error.InvalidStateException
import webauthnkit.core.ctap.ble.BleEvent
import webauthnkit.core.ctap.ble.peripheral.annotation.*
import webauthnkit.core.util.WAKLogger
import java.lang.reflect.Method
import java.util.*

open class PeripheralService(
    val uuidString: String,
    val primary:    Boolean
) {

    val uuid = UUID.fromString(uuidString)

    companion object {
        val TAG = PeripheralService::class.simpleName
    }

    private var analyzed: Boolean = false

    private val characteristics: MutableMap<String, Characteristic> = mutableMapOf()

    internal fun canHandle(event: BleEvent, uuid: String): Boolean {
        return if (characteristics.containsKey(uuid)) {
            characteristics.getValue(uuid).canHandle(event)
        } else {
            false
        }
    }

    internal fun notifyValue(server: BluetoothGattServer,
                             rawCh: BluetoothGattCharacteristic,
                             value: ByteArray) {
        val ch = characteristics[rawCh.uuid.toString()] ?: return
        rawCh.value = value
        ch.getDevicesToNotify().forEach {
            server.notifyCharacteristicChanged(it, rawCh, false)
        }
    }

    internal fun dispatchReadRequest(req: ReadRequest, res: ReadResponse) {
        if (!canHandle(BleEvent.READ, req.uuid)) {
            return
        }
        characteristics.getValue(req.uuid).handleReadRequest(this, req, res)
    }

    internal fun dispatchWriteRequest(req: WriteRequest, res: WriteResponse) {
        if (!canHandle(BleEvent.WRITE, req.uuid)) {
            return
        }
        val ch = characteristics.getValue(req.uuid)
        ch.handleWriteRequest(this, req, res)
    }

    fun rememberDeviceForNotification(device: BluetoothDevice, chUUID: String) {
        characteristics[chUUID]?.rememberDeviceForNotification(device)
    }

    fun forgetDeviceForNotification(device: BluetoothDevice) {
        characteristics.values.forEach {
            it.forgetDeviceForNotification(device)
        }
    }

    private fun serviceType(): Int {
        return if (primary) {
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        } else {
            BluetoothGattService.SERVICE_TYPE_SECONDARY
        }
    }

    internal fun createRaw(): BluetoothGattService {
        val service = BluetoothGattService(uuid, serviceType())
        characteristics.values.forEach {
            service.addCharacteristic(it.createRaw())
        }
        return service
    }

    internal fun analyzeCharacteristicsDefinition() {

        if (analyzed) {
            return
        }

        analyzed = true

        this::class.java.methods.forEach {

            val readAnnotation: OnRead? = it.getAnnotation(OnRead::class.java)
            if (readAnnotation != null) {
                WAKLogger.d(TAG, "found a method set @OnRead")
                if (validReadHandler(it)) {
                    val ch = getOrCreateCharacteristic(readAnnotation.uuid.toLowerCase())
                    ch.addHandler(BleEvent.READ, it)
                    ch.addProperty(BluetoothGattCharacteristic.PROPERTY_READ)
                    val secure: Secure? = it.getAnnotation(Secure::class.java)
                    if (secure != null) {
                        when {
                            secure.value == 0 -> ch.addPermission(BluetoothGattCharacteristic.PERMISSION_READ)
                            secure.value == 1 -> ch.addPermission(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED)
                            secure.value == 2 -> ch.addPermission(BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM)
                        }
                    } else {
                        ch.addPermission(BluetoothGattCharacteristic.PERMISSION_READ)
                    }
                    ch.addPermission(BluetoothGattCharacteristic.PERMISSION_READ)
                    val notifiable: Notifiable? = it.getAnnotation(Notifiable::class.java)
                    if (notifiable != null && notifiable.value) {
                        WAKLogger.d(TAG, "set as Notifiable")
                        ch.addProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)
                    }
                } else {
                    // TODO better named exception
                    throw InvalidStateException("Method definition is invalid for @OnRead")
                }
            }

            val writeAnnotation: OnWrite? = it.getAnnotation(OnWrite::class.java)
            if (writeAnnotation != null) {
                WAKLogger.d(TAG, "found a method set @OnWrite")

                if (validWriteHandler(it)) {

                    val ch = getOrCreateCharacteristic(writeAnnotation.uuid.toLowerCase())
                    ch.addHandler(BleEvent.WRITE, it)

                    val secure: Secure? = it.getAnnotation(Secure::class.java)
                    if (secure != null) {
                        when {
                            secure.value == 0 -> ch.addPermission(BluetoothGattCharacteristic.PERMISSION_WRITE)
                            secure.value == 1 -> ch.addPermission(BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED)
                            secure.value == 2 -> ch.addPermission(BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM)
                        }
                    } else {
                        ch.addPermission(BluetoothGattCharacteristic.PERMISSION_WRITE)
                    }

                    val responseNeeded: ResponseNeeded? = it.getAnnotation(ResponseNeeded::class.java)
                    if (responseNeeded != null) {
                        if (responseNeeded.value) {
                            ch.addProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)
                        } else {
                            ch.addProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
                        }
                    } else {
                        ch.addProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)
                        ch.addProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
                    }

                } else {
                    // TODO better named exception
                    throw InvalidStateException("Method definition is invalid for @OnWrite")
                }
            }
        }
    }

    private fun getOrCreateCharacteristic(uuid: String): Characteristic {
        if (!characteristics.containsKey(uuid)) {
            characteristics[uuid] = Characteristic(uuid)
        }
        return characteristics[uuid]!!
    }

    private fun validReadHandler(method: Method): Boolean {
        val argTypes = method.parameterTypes
        if (argTypes.size != 2) {
            WAKLogger.d(TAG, "arg size is not 2")
            return false
        }
        return (argTypes[0] == ReadRequest::class.java && argTypes[1] == ReadResponse::class.java)
    }

    private fun validWriteHandler(method: Method): Boolean {
        val argTypes = method.parameterTypes
        if (argTypes.size != 2) {
            WAKLogger.d(TAG, "arg size is not 2")
            return false
        }
        return (argTypes[0] == WriteRequest::class.java && argTypes[1] == WriteResponse::class.java)
    }

}