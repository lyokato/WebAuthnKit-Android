package webauthnkit.core.ctap.ble

import kotlinx.coroutines.ExperimentalCoroutinesApi
import webauthnkit.core.data.*

import webauthnkit.core.authenticator.Authenticator
import webauthnkit.core.ctap.ble.operation.CreateOperation
import webauthnkit.core.ctap.ble.operation.GetOperation
import webauthnkit.core.client.operation.OperationListener
import webauthnkit.core.client.operation.OperationType
import webauthnkit.core.util.WAKLogger

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class BLEFIDOOperationManager(
    val authenticator: Authenticator
): OperationListener {

    companion object {
        val TAG = BLEFIDOOperationManager::class.simpleName
    }

    private val getOperations: MutableMap<String, GetOperation> = HashMap()
    private val createOperations: MutableMap<String, CreateOperation> = HashMap()

    suspend fun get(
        clientDataHash:          ByteArray,
        rpId:                    String,
        allowList:               List<PublicKeyCredentialDescriptor>,
        requireUserVerification: Boolean, // op.uv
        requireUserPresence:     Boolean, // op.rk
        timeout:                 Long
    ): ByteArray {

        WAKLogger.d(TAG, "get")

        val session = authenticator.newGetAssertionSession()
        val op = GetOperation(
            clientDataHash          = clientDataHash,
            rpId                    = rpId,
            allowCredential         = allowList,
            session                 = session,
            requireUserVerification = requireUserVerification,
            requireUserPresence     = requireUserPresence,
            lifetimeTimer           = timeout
        )
        op.listener = this
        getOperations[op.opId] = op
        return op.start()
    }

    suspend fun create(
        clientDataHash:          ByteArray,
        rp:                      PublicKeyCredentialRpEntity,
        user:                    PublicKeyCredentialUserEntity,
        pubKeyCredParams:        List<PublicKeyCredentialParameters>,
        requireUserVerification: Boolean, // op.uv
        requireResidentKey:      Boolean, // op.rk
        timeout:                 Long
    ): ByteArray {

        WAKLogger.d(TAG, "create")

        val session = authenticator.newMakeCredentialSession()
        val op = CreateOperation(
            clientDataHash          = clientDataHash,
            rp                      = rp,
            user                    = user,
            pubKeyCredParams        = pubKeyCredParams,
            requireUserVerification = requireUserVerification, // op.uv
            requireResidentKey      = requireResidentKey,      // op.rk
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
