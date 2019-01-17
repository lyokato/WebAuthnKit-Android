package webauthnkit.core.client

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.stringify

import webauthnkit.core.CollectedClientData
import webauthnkit.core.CollectedClientDataType
import webauthnkit.core.PublicKeyCredentialCreationOptions
import webauthnkit.core.PublicKeyCredentialRequestOptions
import webauthnkit.core.authenticator.Authenticator
import webauthnkit.core.client.operation.CreateOperation
import webauthnkit.core.client.operation.GetOperation
import webauthnkit.core.util.WKLogger
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
        WKLogger.d(TAG, "get")

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
        WKLogger.d(TAG, "create")

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
        WKLogger.d(TAG, "adjustLifetimeTimer")
        return timeout?.let { t ->
            return when {
                t < minTimeout -> minTimeout
                t > maxTimeout -> maxTimeout
                else -> t
            }
        } ?: defaultTimeout
    }

    private fun pickRelyingPartyID(rpId: String?): String {
        WKLogger.d(TAG, "pickRelyingPartyID")
        return rpId?.let { it } ?: origin
    }

    private fun generateClientData(
        type: CollectedClientDataType,
        challenge: String
    ): Triple<CollectedClientData, String, UByteArray> {

        WKLogger.d(TAG, "generateClientData")

        val data = CollectedClientData(
            type      = type,
            challenge = challenge,
            origin    = origin
        )

        val json = JSON.stringify(data)
        val hash = ByteArrayUtil.sha256(json).toUByteArray()

        return Triple(data, json, hash)
    }
}
