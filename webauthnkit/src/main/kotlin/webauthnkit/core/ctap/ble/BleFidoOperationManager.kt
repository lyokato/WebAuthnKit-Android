package webauthnkit.core.ctap.ble

import kotlinx.coroutines.ExperimentalCoroutinesApi

import webauthnkit.core.authenticator.Authenticator
import webauthnkit.core.ctap.ble.operation.CreateOperation
import webauthnkit.core.ctap.ble.operation.GetOperation
import webauthnkit.core.client.operation.OperationListener
import webauthnkit.core.client.operation.OperationType
import webauthnkit.core.ctap.options.GetAssertionOptions
import webauthnkit.core.ctap.options.MakeCredentialOptions
import webauthnkit.core.util.WAKLogger

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class BleFidoOperationManager(
    val authenticator: Authenticator
): OperationListener {

    companion object {
        val TAG = BleFidoOperationManager::class.simpleName
    }

    private val getOperations: MutableMap<String, GetOperation> = HashMap()
    private val createOperations: MutableMap<String, CreateOperation> = HashMap()

    fun hasActiveOperation(): Boolean {
        return (getOperations.isNotEmpty() || createOperations.isNotEmpty())
    }

    suspend fun get(
        options: GetAssertionOptions,
        timeout: Long
    ): ByteArray {

        WAKLogger.d(TAG, "get")

        val session = authenticator.newGetAssertionSession()
        val op = GetOperation(
            options       = options,
            session       = session,
            lifetimeTimer = timeout
        )
        op.listener = this
        getOperations[op.opId] = op
        return op.start()
    }

    suspend fun create(
        options: MakeCredentialOptions,
        timeout: Long
    ): ByteArray {

        WAKLogger.d(TAG, "create")

        val session = authenticator.newMakeCredentialSession()
        val op = CreateOperation(
            options                 = options,
            session                 = session,
            lifetimeTimer           = timeout
        )
        op.listener = this
        createOperations[op.opId] = op
        return op.start()
    }


    fun cancel() {
        WAKLogger.d(TAG, "cancel")
        getOperations.forEach { it.value.cancel()}
        createOperations.forEach { it.value.cancel()}
    }

    override fun onFinish(opType: OperationType, opId: String) {
        WAKLogger.d(TAG, "operation finished")
        when (opType) {
            OperationType.Get -> {
                if (getOperations.containsKey(opId)) {
                    getOperations.remove(opId)
                }
            }
            OperationType.Create -> {
                if (createOperations.containsKey(opId)) {
                    createOperations.remove(opId)
                }
            }
        }
    }

}
