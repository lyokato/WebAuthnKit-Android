package webauthnkit.core.ctap.ble.peripheral.annotation


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class OnWrite(val uuid: String)