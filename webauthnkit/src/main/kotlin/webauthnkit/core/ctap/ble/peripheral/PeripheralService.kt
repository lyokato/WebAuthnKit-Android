package webauthnkit.core.ctap.ble.peripheral

import java.util.*


class PeripheralService(val uuid: UUID) {

    companion object {
        val TAG = PeripheralService::class.simpleName
    }

    fun init() {
        // template method
    }

    val characteristics: Map<String, Characteristic> = HashMap()

    fun canHandle(event: BLEEvent, uuid: String): Boolean {
        return if (characteristics.containsKey(uuid)) {
            characteristics.getValue(uuid).canHandle(event)
        } else {
            false
        }
    }

    fun dispatchReadRequest(req: ReadRequest, res: ReadResponse) {
        if (!canHandle(BLEEvent.READ, req.uuid)) {
            return
        }
        characteristics.getValue(req.uuid).handleReadRequest(this, req, res)
    }

    fun dispatchWriteRequest(req: WriteRequest, res: WriteResponse) {

    }

}