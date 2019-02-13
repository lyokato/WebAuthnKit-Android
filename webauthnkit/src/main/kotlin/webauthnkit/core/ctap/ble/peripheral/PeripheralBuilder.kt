package webauthnkit.core.ctap.ble.peripheral

import android.content.Context

class PeripheralBuilder(
    private val context: Context,
    private val listener: PeripheralListener
) {

    private val services: MutableMap<String, PeripheralService> = mutableMapOf()

    fun service(service: PeripheralService): PeripheralBuilder {
        service.analyzeCharacteristicsDefinition()
        services[service.uuidString.toLowerCase()] = service
        return this
    }

    fun build(): Peripheral {
        return Peripheral(
            context = context,
            services = services,
            listener = listener
        )
    }

}