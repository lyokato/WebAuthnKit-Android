package webauthnkit.core.ctap.ble.peripheral

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import webauthnkit.core.ctap.ble.BLEEvent
import webauthnkit.core.util.WAKLogger
import java.util.*

interface PeripheralListener {
    fun onAdvertiseSuccess(settingsInEffect: AdvertiseSettings)
    fun onAdvertiseFailure(errorCode: Int)
    fun onConnected(address: String)
    fun onDisconnected(address: String)
    fun onMtuChanged(device: BluetoothDevice, mtu: Int)
}

class Peripheral(
    private val context:  Context,
    private val service:  PeripheralService,
    private var listener: PeripheralListener?
) {

    companion object {
        val TAG = Peripheral::class.simpleName
    }

    init {
        service.analyzeCharacteristicsDefinition()
    }

    private var running: Boolean = false
    private var manager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    private var adapter: BluetoothAdapter? = manager?.adapter

    private var rawServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null

    var advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED
    var advertiseTxPower = AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM

    var includeTxPower = false
    var includeDeviceName = false

    fun systemSupported(): Boolean {

        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            WAKLogger.w(TAG, "Feature not supported")
            return false
        }
        if (manager == null) {
            WAKLogger.w(TAG, "Manager not found")
            return false
        }

        if (adapter == null) {
            WAKLogger.w(TAG, "Adapter not found")
            return false
        }

        if (!(adapter!!.isMultipleAdvertisementSupported)) {
            return false
        }

        return true
    }

    fun isEnabled(): Boolean {
        return (adapter != null && adapter!!.isEnabled)
    }

    fun start(): Boolean {
        WAKLogger.d(TAG, "start")

        if (running) {
            WAKLogger.d(TAG, "already running")
            return false
        }

        if (!(systemSupported() && isEnabled())) {
            WAKLogger.w(TAG, "can't run")
            return false
        }

        rawServer = manager!!.openGattServer(context, createServerCallback())
        if (rawServer == null) {
            WAKLogger.w(TAG, "can't open Gatt Server")
            return false
        }

        rawServer!!.addService(service.createRaw())

        advertiser = adapter!!.bluetoothLeAdvertiser
        if (advertiser == null) {
            WAKLogger.w(TAG, "can't get LE Advertiser")
            return false
        }

        advertiseCallback = createAdvertiseCallback()
        advertiser!!.startAdvertising(
            createAdvertiseSettings(),
            createAdvertiseData(),
            advertiseCallback)

        running = true
        return true
    }

    fun isRunning(): Boolean {
        return running
    }

    fun stop() {
        WAKLogger.d(TAG, "stop")
        if (!running) {
            WAKLogger.d(TAG, "not running")
            return
        }

        rawServer?.let {
            it.clearServices()
            it.close()
        }
        rawServer = null

        advertiser?.stopAdvertising(advertiseCallback)
        advertiser = null

        running = false
    }

    fun notifyValue(serviceUUIDString: String, chUUIDString: String, value: ByteArray) {

        if (rawServer == null) {
            WAKLogger.d(TAG, "server not found")
            return
        }

        val serviceUUID = UUID.fromString(serviceUUIDString)
        val chUUID = UUID.fromString(chUUIDString)

        val rawService = rawServer!!.getService(serviceUUID)
        if (rawService == null) {
            WAKLogger.d(TAG, "service not found")
            return
        }

        val rawCh = rawService.getCharacteristic(chUUID)
        if (rawCh == null) {
            WAKLogger.d(TAG, "characteristic not found")
            return
        }

        service.notifyValue(rawServer!!, rawCh, value)

    }

    private fun createAdvertiseData(): AdvertiseData {
        val builder=  AdvertiseData.Builder()
        builder.setIncludeTxPowerLevel(includeTxPower)
        builder.setIncludeDeviceName(includeDeviceName)
        builder.addServiceUuid(ParcelUuid(service.uuid))
        return builder.build()
    }

    private fun createAdvertiseSettings(): AdvertiseSettings {
        val builder = AdvertiseSettings.Builder()
        builder.setConnectable(true)
        builder.setTxPowerLevel(this.advertiseTxPower)
        builder.setAdvertiseMode(this.advertiseMode)
        return builder.build()
    }

    private fun createAdvertiseCallback(): AdvertiseCallback {
        return object: AdvertiseCallback() {

            override fun onStartFailure(errorCode: Int) {
                listener?.onAdvertiseFailure(errorCode)
            }

            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                listener?.onAdvertiseSuccess(settingsInEffect)
            }

        }

    }

    private fun createServerCallback(): BluetoothGattServerCallback {

        return object: BluetoothGattServerCallback() {

            override fun onCharacteristicReadRequest(
                device:         BluetoothDevice,
                requestId:      Int,
                offset:         Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                WAKLogger.d(TAG, "onCharacteristicReadRequest")

                val req = ReadRequest(device, characteristic, requestId, offset)
                val res = ReadResponse(req)

                val serviceUUID = characteristic.service.uuid
                if (serviceUUID.toString() == service.uuidString) {
                    if (service.canHandle(BLEEvent.READ, req.uuid)) {
                        service.dispatchReadRequest(req, res)
                    }
                }

                res.finishOn(rawServer!!)
            }

            override fun onCharacteristicWriteRequest(
                device:         BluetoothDevice,
                requestId:      Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite:  Boolean,
                responseNeeded: Boolean,
                offset:         Int,
                value:          ByteArray
            ) {
                WAKLogger.d(TAG, "onCharacteristicWriteRequest")

                val req = WriteRequest(device, characteristic, requestId,
                    offset, preparedWrite, responseNeeded, value)
                val res = WriteResponse(req)

                val serviceUUID = characteristic.service.uuid
                if (serviceUUID.toString() == service.uuidString) {
                    if (service.canHandle(BLEEvent.WRITE, req.uuid)) {
                        service.dispatchWriteRequest(req, res)
                    }
                }

                res.finishOn(rawServer!!)
            }

            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                WAKLogger.d(TAG, "onConnectionStateChange")

                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    service.forgetDeviceForNotification(device)
                    listener?.onDisconnected(device.address)
                } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // TODO needed? device.createBond()
                    listener?.onConnected(device.address)
                }
            }

            override fun onDescriptorReadRequest(
                device:     BluetoothDevice,
                requestId:  Int,
                offset:     Int,
                descriptor: BluetoothGattDescriptor
            ) {
                WAKLogger.d(TAG, "onDescriptionReadRequest")
            }

            override fun onDescriptorWriteRequest(
                device:         BluetoothDevice,
                requestId:      Int,
                descriptor:     BluetoothGattDescriptor,
                preparedWrite:  Boolean,
                responseNeeded: Boolean,
                offset:         Int,
                value:          ByteArray
            ) {
                WAKLogger.d(TAG, "onDescriptionWriteRequest")

                if (descriptor.uuid.toString() == "00002902-0000-1000-8000-00805F9B34FB"
                    && Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {

                    val serviceUUID = descriptor.characteristic.service.uuid
                    val chUUID = descriptor.characteristic.uuid

                    WAKLogger.d(TAG, "onDescriptionWriteRequest: $chUUID")

                    if (service.uuidString == serviceUUID.toString()) {
                        service.rememberDeviceForNotification(device, chUUID.toString())
                    }

                }
                rawServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }

            override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
                WAKLogger.d(TAG, "onExecuteWrite")
            }

            override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
                WAKLogger.d(TAG, "onMtuChanged")
                listener?.onMtuChanged(device, mtu)
            }

            override fun onNotificationSent(device: BluetoothDevice, status: Int) {
                WAKLogger.d(TAG, "onNotificationSent")
            }

            override fun onPhyRead(device: BluetoothDevice, txPhy: Int, rxPhy: Int, status: Int) {
                WAKLogger.d(TAG, "onPhyRead")
            }

            override fun onPhyUpdate(device: BluetoothDevice, txPhy: Int, rxPhy: Int, status: Int) {
                WAKLogger.d(TAG, "onPhyUpdate")
            }

            override fun onServiceAdded(status: Int, service: BluetoothGattService) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    WAKLogger.d(TAG, "BLE service added")
                } else {
                    WAKLogger.d(TAG, "BLE service not added")
                }
            }

        }
    }
}