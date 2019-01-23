package webauthnkit.core.client

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ImplicitReflectionSerializer

import webauthnkit.core.CollectedClientData
import webauthnkit.core.CollectedClientDataType
import webauthnkit.core.PublicKeyCredentialCreationOptions
import webauthnkit.core.PublicKeyCredentialRequestOptions
import webauthnkit.core.authenticator.Authenticator
import webauthnkit.core.client.operation.CreateOperation
import webauthnkit.core.client.operation.GetOperation
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.util.ByteArrayUtil

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
class WebAuthnClient(
    private val authenticator: Authenticator,
    private val origin:        String
) {

    companion object {
        val TAG = this::class.simpleName
    }

    var defaultTimeout: Long = 60
    var minTimeout:     Long = 15
    var maxTimeout:     Long = 120

    fun get(options: PublicKeyCredentialRequestOptions): GetOperation {
        WAKLogger.d(TAG, "get")

        val timer = adjustLifetimeTimer(options.timeout)
        val rpId  = pickRelyingPartyID(options.rpId)

        val (data, json, hash) =
                generateClientData(
                    type      = CollectedClientDataType.Create,
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

    fun create(options: PublicKeyCredentialCreationOptions): CreateOperation {
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
        //val json = JSON.stringify(data)
        return ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL).writeValueAsString(data)
    }
}
