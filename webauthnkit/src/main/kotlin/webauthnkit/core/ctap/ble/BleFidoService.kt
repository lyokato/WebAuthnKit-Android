package webauthnkit.core.ctap.ble

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.AdvertiseSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import webauthnkit.core.authenticator.Authenticator
import webauthnkit.core.ctap.CTAPCommandType
import webauthnkit.core.ctap.ble.frame.FrameBuffer
import webauthnkit.core.ctap.ble.frame.FrameSplitter
import webauthnkit.core.ctap.ble.peripheral.*
import webauthnkit.core.ctap.ble.peripheral.annotation.*
import webauthnkit.core.ctap.options.GetAssertionOptions
import webauthnkit.core.ctap.options.MakeCredentialOptions
import webauthnkit.core.util.CBORWriter
import webauthnkit.core.util.WAKLogger

interface BleFidoServiceListener {
    fun onConnected(address: String)
    fun onDisconnected(address: String)
    fun onClosed()
}

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class BleFidoService(
    private val activity:      Activity,
                authenticator: Authenticator,
    private var listener:      BleFidoServiceListener?
) {

    private val operationManager = BleFidoOperationManager(authenticator)

    var fragmentedResponseIntervalMilliSeconds: Long = 20
    var maxPacketDataSize: Int = 20
    private var lockedByDevice: String? = null

    companion object {
        val TAG = BleFidoService::class.simpleName
        // 16 bit service UUID (FFFD)
        val FIDO_UUID = "0000FFFD-0000-1000-8000-00805F9B34FB".toLowerCase()
    }

    private val peripheralListener = object: PeripheralListener {

        override fun onAdvertiseSuccess(settingsInEffect: AdvertiseSettings) {
            WAKLogger.d(TAG, "onAdvertiseSuccess")
        }

        override fun onAdvertiseFailure(errorCode: Int) {
            WAKLogger.d(TAG, "onAdvertiseFailure: $errorCode")
            close()
        }

        override fun onConnected(address: String) {
            WAKLogger.d(TAG, "onConnected: $address")

            if (lockedByDevice == null) {
                lockedByDevice = address
                activity.runOnUiThread {
                    listener?.onConnected(address)
                }
            } else {
                WAKLogger.d(TAG, "onConnected: this device is already locked by $lockedByDevice")
            }
        }

        override fun onDisconnected(address: String) {
            WAKLogger.d(TAG, "onDisconnected: $address")

            if (isLockedBy(address)) {
                activity.runOnUiThread {
                    listener?.onDisconnected(address)
                }
                close()
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            WAKLogger.d(TAG, "onMtuChanged")
            if (isLockedBy(device.address)) {
                maxPacketDataSize = mtu - 3
            }
        }
    }

    private fun isLockedBy(deviceAddress: String): Boolean {
        return (lockedByDevice != null && lockedByDevice == deviceAddress)
    }

    fun start(): Boolean {
        this.peripheral = createPeripheral()
        return if (peripheral!!.start()) {
            true
        } else {
            peripheral?.stop()
            peripheral = null
            false
        }
    }

    fun stop() {
        close()
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
        operationManager.cancel()
    }

    private fun handleBLEKeepAlive(value: ByteArray) {
        WAKLogger.d(TAG, "handleBLE: KeepAlive")
        WAKLogger.d(TAG, "should be authenticator -> client")
        closeByBLEError(BLEErrorType.InvalidCmd)
    }

    private fun handleBLEMSG(value: ByteArray) {

        WAKLogger.d(TAG, "handleBLE: MSG")

        if (value.isEmpty()) {
            closeByBLEError(BLEErrorType.InvalidLen)
            return
        }

        val command = CTAPCommandType.fromByte(value[0])
        if (command == null) {
            handleCTAPUnsupportedCommand()
            return
        }

        when (command) {

            CTAPCommandType.MakeCredential -> {
                if (value.size < 2) {
                    closeByBLEError(BLEErrorType.InvalidLen)
                    return
                }
                handleCTAPMakeCredential(value.sliceArray(0..value.size))
            }

            CTAPCommandType.GetAssertion -> {
                if (value.size < 2) {
                    closeByBLEError(BLEErrorType.InvalidLen)
                    return
                }
                handleCTAPGetAssertion(value.sliceArray(0..value.size))
            }

            CTAPCommandType.GetNextAssertion -> {
                handleCTAPGetNextAssertion()
            }

            CTAPCommandType.ClientPIN -> {
                if (value.size < 2) {
                    closeByBLEError(BLEErrorType.InvalidLen)
                    return
                }
                handleCTAPClientPIN(value.sliceArray(0..value.size))
            }

            CTAPCommandType.GetInfo -> {
                handleCTAPGetInfo()
            }

            CTAPCommandType.Reset -> {
                handleCTAPReset()
            }

        }
    }

    private fun handleBLEError(value: ByteArray) {
        WAKLogger.d(TAG, "handleBLE: Error")
        closeByBLEError(BLEErrorType.InvalidCmd)
    }

    private fun handleBLEPing(value: ByteArray) {
        WAKLogger.d(TAG, "handleBLE: Ping")
    }

    private var isInSession: Boolean = false

    private fun handleCTAPMakeCredential(value: ByteArray) {

        WAKLogger.d(TAG, "handleCTAP: MakeCredential")

        if (operationManager.hasActiveOperation()) {
            WAKLogger.d(TAG, "handleCTAP: MakeCredential - busy")
            // TODO better error message
            closeByBLEError(BLEErrorType.Other)
            return
        }

        val (options, error) =
            MakeCredentialOptions.fromByteArray(value)

        if (error != null) {
            closeByBLEError(error)
            return
        }

        if (isInSession) {
            WAKLogger.d(TAG, "handleCTAP: MakeCredential - already in session")
            return
        }

        isInSession = true

        GlobalScope.launch {
            try {
                val res = operationManager.create(
                    options = options!!,
                    timeout = 60000L
                )
                handleResponse(BLECommandType.MSG, res)
            } catch (e: Exception) {
                // TODO Proper Processing Error
                closeByBLEError(BLEErrorType.Other)
            } finally {
                isInSession = false
            }
        }
    }

    private fun handleCTAPGetAssertion(value: ByteArray) {

        WAKLogger.d(TAG, "handleCTAP: GetAssertion")

        if (operationManager.hasActiveOperation()) {
            WAKLogger.d(TAG, "handleCTAP: MakeCredential - busy")
            // TODO better error message
            closeByBLEError(BLEErrorType.Other)
            return
        }

        val (options, error) =
            GetAssertionOptions.fromByteArray(value)

        if (error != null) {
            closeByBLEError(error)
            return
        }

        if (isInSession) {
            WAKLogger.d(TAG, "handleCTAP: GetAssertion - already in session")
            return
        }

        isInSession = true

        GlobalScope.launch {
            try {
                val res = operationManager.get(
                    options = options!!,
                    timeout = 60000L
                )
                handleResponse(BLECommandType.MSG, res)
            } catch (e: Exception) {
                // TODO Proper Processing Error
                closeByBLEError(BLEErrorType.Other)
            } finally {
                isInSession = false
            }
        }
    }

    private fun handleCTAPGetInfo() {
        WAKLogger.d(TAG, "handleCTAP: GetInfo")

        val info: MutableMap<String, Any> = mutableMapOf()

        info["versions"] = "FIDO_2_0"

        info["aaguid"] = byteArrayOf(
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        )

        val options: MutableMap<String, Any> = mutableMapOf()
        options["plat"] = false
        options["rk"]   = true
        options["up"]   = true
        options["uv"]   = true

        info["options"] = options

        // info["maxMsgSize"] = maxMsgSize
        // info["extensions"]  // currently this library doesn't support any extensions

        val value = CBORWriter().putStringKeyMap(info).compute()
        handleResponse(BLECommandType.MSG, value)
    }

    private var peripheral: Peripheral? = null

    private fun handleResponse(command: BLECommandType, value: ByteArray) {

        WAKLogger.d(TAG, "handleResponse")

        val (first, rest) =
            FrameSplitter(maxPacketDataSize).split(command, value)

        GlobalScope.launch {

            sendResultAsNotification(first.toByteArray())

            rest.forEach {
                delay(fragmentedResponseIntervalMilliSeconds)
                sendResultAsNotification(it.toByteArray())
            }

        }

    }

    private fun sendResultAsNotification(value: ByteArray) {

        WAKLogger.d(TAG, "sendResultAsNotification")

        peripheral?.notifyValue(
                FIDO_UUID,
                "F1D0FFF1-DEAA-ECEE-B42F-C9BA7ED623BB",
                 value
        )
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
        closeByBLEError(BLEErrorType.InvalidCmd)
    }

    private fun closeByBLEError(error: BLEErrorType) {

        WAKLogger.d(TAG, "closeByBLEError")

        val b1 = (error.rawValue and 0x0000_ff00).shr(8).toByte()
        val b2= (error.rawValue and 0x0000_00ff).toByte()
        val value = byteArrayOf(b1, b2)

        val (first, rest) =
            FrameSplitter(maxPacketDataSize).split(BLECommandType.Error, value)

        GlobalScope.launch {

            sendResultAsNotification(first.toByteArray())

            rest.forEach {
                delay(fragmentedResponseIntervalMilliSeconds)
                sendResultAsNotification(it.toByteArray())
            }

            delay(fragmentedResponseIntervalMilliSeconds)
            close()

        }

    }

    var closed = false

    private fun close() {

        WAKLogger.d(TAG, "close")

        if (closed) {
            WAKLogger.d(TAG, "already closed")
            return
        }

        closed = true

        peripheral?.stop()
        peripheral = null

        activity.runOnUiThread {
            listener?.onClosed()
            listener = null
        }
    }

    private var frameBuffer = FrameBuffer()

    private fun createPeripheral(): Peripheral {

        val fidoService = object: PeripheralService(FIDO_UUID, true) {

            @OnWrite("F1D0FFF1-DEAA-ECEE-B42F-C9BA7ED623BB")
            @ResponseNeeded(true)
            @Secure(false)
            fun controlPoint(req: WriteRequest, res: WriteResponse) {
                WAKLogger.d(TAG, "@Write: controlPoint")

                if (!isLockedBy(req.device.address)) {
                    WAKLogger.d(TAG, "@Write: unbound device")
                    res.status = BluetoothGatt.GATT_FAILURE
                    return
                }

                GlobalScope.launch {

                    val error = frameBuffer.putFragment(req.value)

                    error?.let {
                        frameBuffer.clear()
                        closeByBLEError(it)
                        return@launch
                    }

                    if (frameBuffer.isDone()) {
                        handleCommand(frameBuffer.getCommand(), frameBuffer.getData())
                        frameBuffer.clear()
                    }

                }

            }

            @OnRead("F1D0FFF2-DEAA-ECEE-B42F-C9BA7ED623BB")
            @Notifiable(true)
            @Secure(false)
            fun status(req: ReadRequest, res: ReadResponse) {
                WAKLogger.d(TAG, "@Read: status")
                WAKLogger.d(TAG, "This characteristic is just for notification")
                res.status = BluetoothGatt.GATT_FAILURE
                return
            }

            @OnRead("F1D0FFF3-DEAA-ECEE-B42F-C9BA7ED623BB")
            @Secure(false)
            fun controlPointLength(req: ReadRequest, res: ReadResponse) {
                WAKLogger.d(TAG, "@Read: controlPointLength")
                if (!isLockedBy(req.device.address)) {
                    WAKLogger.d(TAG, "@Write: unbound device")
                    res.status = BluetoothGatt.GATT_FAILURE
                    return
                }
                val b1 = (maxPacketDataSize and 0x0000_ff00).shr(8).toByte()
                val b2 = (maxPacketDataSize and 0x0000_00ff).toByte()
                res.write(byteArrayOf(b1, b2))
            }

            @OnWrite("F1D0FFF4-DEAA-ECEE-B42F-C9BA7ED623BB")
            @ResponseNeeded(true)
            @Secure(false)
            fun serviceRevisionBitFieldWrite(req: WriteRequest, res: WriteResponse) {
                WAKLogger.d(TAG, "@Write: serviceRevisionBitField")

                if (!isLockedBy(req.device.address)) {
                    WAKLogger.d(TAG, "@Write: unbound device")
                    res.status = BluetoothGatt.GATT_FAILURE
                    return
                }

                if (req.value.size == 1 && req.value[0].toInt() == 0x20) {
                    res.status = BluetoothGatt.GATT_SUCCESS
                } else {
                    WAKLogger.d(TAG, "@unsupported bitfield")
                    res.status = BluetoothGatt.GATT_FAILURE
                }
            }

            @OnRead("F1D0FFF4-DEAA-ECEE-B42F-C9BA7ED623BB")
            @Secure(false)
            fun serviceRevisionBitFieldRead(req: ReadRequest, res: ReadResponse) {
                WAKLogger.d(TAG, "@Read: serviceRevisionBitField")

                if (!isLockedBy(req.device.address)) {
                    WAKLogger.d(TAG, "@Write: unbound device")
                    res.status = BluetoothGatt.GATT_FAILURE
                    return
                }

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

        return PeripheralBuilder(activity.applicationContext, peripheralListener)
            .service(fidoService)
            .build()

    }

}