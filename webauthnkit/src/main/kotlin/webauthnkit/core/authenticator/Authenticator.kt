package webauthnkit.core.authenticator

import webauthnkit.core.*

@ExperimentalUnsignedTypes
class AuthenticatorAssertionResult(
    var credentialId:      UByteArray?,
    var userHandle:        UByteArray?,
    var signature:         UByteArray,
    var authenticatorData: UByteArray
)

@ExperimentalUnsignedTypes
interface MakeCredentialSessionListener {
    fun onAvailable(session: MakeCredentialSession) {}
    fun onUnavailable(session: MakeCredentialSession) {}
    fun onOperationStopped(session: MakeCredentialSession, reason: ErrorReason) {}
    fun onCredentialCreated(session: MakeCredentialSession, attestationObject: AttestationObject) {}
}

@ExperimentalUnsignedTypes
interface GetAssertionSessionListener {
    fun onAvailable(session: GetAssertionSession) {}
    fun onUnavailable(session: GetAssertionSession) {}
    fun onOperationStopped(session: GetAssertionSession, reason: ErrorReason) {}
    fun onCredentialDiscovered(session: GetAssertionSession, assertion: AuthenticatorAssertionResult) {}
}

@ExperimentalUnsignedTypes
interface GetAssertionSession {

    val attachment: AuthenticatorAttachment
    val transport: AuthenticatorTransport
    var listener: GetAssertionSessionListener?

    fun getAssertion(
        rpId: String,
        hash: UByteArray,
        allowCredentialDescriptorList: List<PublicKeyCredentialDescriptor>,
        requireUserPresence: Boolean,
        requireUserVerification: Boolean
        // extensions: Map<String, Any>
    )
    fun canPerformUserVerification(): Boolean
    fun start()
    fun cancel(reason: ErrorReason)
}

@ExperimentalUnsignedTypes
interface MakeCredentialSession {

    val attachment: AuthenticatorAttachment
    val transport: AuthenticatorTransport
    var listener: MakeCredentialSessionListener?

    fun makeCredential(
        hash: UByteArray,
        rpEntity: PublicKeyCredentialRpEntity,
        userEntity: PublicKeyCredentialUserEntity,
        requireResidentKey: Boolean,
        requireUserPresence: Boolean,
        requireUserVerification: Boolean,
        credTypesAndPubKeyAlgs: List<PublicKeyCredentialParameters>,
        excludeCredentialDescriptorList: List<PublicKeyCredentialDescriptor>
    )
    fun canPerformUserVerification(): Boolean
    fun canStoreResidentKey(): Boolean
    fun start()
    fun cancel(reason: ErrorReason)
}

@ExperimentalUnsignedTypes
interface Authenticator {

    val attachment: AuthenticatorAttachment
    val transport: AuthenticatorTransport
    val counterStep: Int
    val allowResidentKey: Boolean
    val allowUserVerification: Boolean

    fun newMakeCredentialSession(): MakeCredentialSession
    fun newGetAssertionSession(): GetAssertionSession
}