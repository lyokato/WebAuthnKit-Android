package webauthnkit.core.client

import androidx.fragment.app.FragmentActivity
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import webauthnkit.core.data.*

import webauthnkit.core.authenticator.Authenticator
import webauthnkit.core.authenticator.internal.InternalAuthenticator
import webauthnkit.core.authenticator.internal.ui.UserConsentUI
import webauthnkit.core.client.operation.CreateOperation
import webauthnkit.core.client.operation.GetOperation
import webauthnkit.core.client.operation.OperationListener
import webauthnkit.core.client.operation.OperationType
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.util.ByteArrayUtil

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class WebAuthnClient(
    val authenticator: Authenticator,
    private val origin:        String
): OperationListener {

    companion object {
        val TAG = WebAuthnClient::class.simpleName

        fun create(
            activity: FragmentActivity,
            origin:   String,
            ui:       UserConsentUI
        ): WebAuthnClient {

            val authenticator = InternalAuthenticator(
                activity = activity,
                ui       = ui
            )

            return WebAuthnClient(
                origin        = origin,
                authenticator = authenticator
            )
        }
    }

    var defaultTimeout: Long = 60
    var minTimeout:     Long = 15
    var maxTimeout:     Long = 120

    private val getOperations: MutableMap<String, GetOperation> = HashMap()
    private val createOperations: MutableMap<String, CreateOperation> = HashMap()

    suspend fun get(options: PublicKeyCredentialRequestOptions): GetAssertionResponse {
        val op = newGetOperation(options)
        op.listener = this
        getOperations[op.opId] = op
        return op.start()
    }

    private fun newGetOperation(options: PublicKeyCredentialRequestOptions): GetOperation {
        WAKLogger.d(TAG, "get")

        val timer = adjustLifetimeTimer(options.timeout)
        val rpId  = pickRelyingPartyID(options.rpId)

        val (data, json, hash) =
                generateClientData(
                    type      = CollectedClientDataType.Get,
                    challenge = ByteArrayUtil.encodeBase64URL(options.challenge)
                )

        val session = authenticator.newGetAssertionSession()

        return GetOperation(
            options        = options,
            rpId           = rpId,
            session        = session,
            clientData     = data,
            clientDataJSON = json,
            clientDataHash = hash,
            lifetimeTimer  = timer
        )
    }

    suspend fun create(options: PublicKeyCredentialCreationOptions): MakeCredentialResponse {
        val op = newCreateOperation(options)
        op.listener = this
        createOperations[op.opId] = op
        return op.start()
    }


    private fun newCreateOperation(options: PublicKeyCredentialCreationOptions): CreateOperation {
        WAKLogger.d(TAG, "create")

        val timer = adjustLifetimeTimer(options.timeout)
        val rpId  = pickRelyingPartyID(options.rp.id)

        val (data, json, hash) =
                generateClientData(
                    type      = CollectedClientDataType.Create,
                    challenge = ByteArrayUtil.encodeBase64URL(options.challenge)
                )

        val session = authenticator.newMakeCredentialSession()

        return CreateOperation(
            options        = options,
            rpId           = rpId,
            session        = session,
            clientData     = data,
            clientDataJSON = json,
            clientDataHash = hash,
            lifetimeTimer  = timer
        )
    }

    fun cancel() {
        WAKLogger.d(TAG, "cancel")
        getOperations.forEach { it.value.cancel()}
        createOperations.forEach { it.value.cancel()}
    }

    private fun adjustLifetimeTimer(timeout: Long?): Long {
        WAKLogger.d(TAG, "adjustLifetimeTimer")
        return timeout?.let { t ->
            return when {
                t < minTimeout -> minTimeout
                t > maxTimeout -> maxTimeout
                else -> t
            }
        } ?: defaultTimeout
    }

    private fun pickRelyingPartyID(rpId: String?): String {
        WAKLogger.d(TAG, "pickRelyingPartyID")
        return rpId?.let { it } ?: origin
    }

    private fun generateClientData(
        type: CollectedClientDataType,
        challenge: String
    ): Triple<CollectedClientData, String, ByteArray> {

        WAKLogger.d(TAG, "generateClientData")

        val data = CollectedClientData(
            type      = type.toString(),
            challenge = challenge,
            origin    = origin
        )

        val json = encodeJSON(data)
        val hash = ByteArrayUtil.sha256(json)

        return Triple(data, json, hash)
    }

    private fun encodeJSON(data: CollectedClientData): String {
        return ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .writeValueAsString(data)
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
