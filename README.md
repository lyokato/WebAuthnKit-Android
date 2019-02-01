
# WebAuthnKit (Android)

This library provides you a way to handle W3C Web Authentication API (a.k.a. WebAuthN / FIDO 2.0) easily.

![android_webauthnkit](https://user-images.githubusercontent.com/30877/52110613-81deba80-2644-11e9-8349-db9880127cfe.jpg)

## Installation

WIP

## Getting Started

### AutoBackup setting

Make sure to eclude 'webauthnkit.db'

- AndroidManifest.xml
```xml
<application
        android:fullBackupContent="@xml/backup_rules">
```

- values/backup_rules.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <exclude domain="database" path="webauthnkit.db" />
</full-backup-content>
```

Or you can set allowBackup="false" simply.

```xml
<application
        android:allowBackup="false">
```

### Activity

WebAuthnKit uses Kotlin's experimental features.
So, add some annotations on your Activity.

`FragmentActivity` is required to be bound with WebAuthnKit's UI features.
Of cource, `androidx.appcompat.app.AppCompatActivity` is also OK.

```kotlin
@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class AuthenticationActivity : AppCompatActivity() {
  //...
}
```

### Setup your WebAuthnClient

At first, prepare UserConsentUI on your Activity.

```kotlin
import webauthnkit.core.authenticator.internal.ui.UserConsentUI
import webauthnkit.core.authenticator.internal.ui.UserConsentUIFactory

var consentUI: UserConsentUI? = null

override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  consentUI = UserConsentUIFactory.create(this)

  // You can configure consent-ui here
  // consentUI.config.registrationDialogTitle = "New Login Key"
  // consentUI.config.selectionDialogTitle = "Select Key"
  // ...
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  consentUI?.onActivityResult(requestCode, resultCode, data)
}
```

Then, create WebAuthnClient

```kotlin
import webauthnkit.core.client.WebAuthnClient

val client = WebAuthnClient.internal(
  activity = this,
  origin   = "https://example.org"
  ui       = consentUI!!
)
// You can configure client here
// client.maxTimeout = 120
// client.defaultTimeout = 60
```

### Registration Flow

With a flow which is described in following documents, WebAuthnClient creates a credential if it succeeded.

- https://www.w3.org/TR/webauthn/#createCredential
- https://www.w3.org/TR/webauthn/#op-make-cred

```kotlin

private suspend fun executeRegistration() {

    val options = PublicKeyCredentialCreationOptions()

    options.challenge        = ByteArrayUtil.fromHex(challenge)
    options.user.id          = userId
    options.user.name        = username
    options.user.displayName = userDisplayName
    options.user.icon        = userIconURL
    options.rp.id            = "https://example.org"
    options.rp.name          = "your_service_name"
    options.rp.icon          = yourServiceIconURL
    options.attestation      = attestationConveyance

    options.addPubKeyCredParam(
        alg = COSEAlgorithmIdentifier.es256
    )

    options.authenticatorSelection = AuthenticatorSelectionCriteria(
        requireResidentKey = true,
        userVerification   = userVerification
    )

    try {

        val credential = client.create(options)

        // send parameters to your server
        // credential.id
        // credential.rawId
        // credential.response.attestationObject
        // credential.response.clientDataJSON

    } catch (e: Exception) {
        // error handling
    }

}

```

If you would like to stop while client is in progress, you can call cancel method.

```kotlin
client.cancel()
```

`webauthnkit.core.CancelledException` will be thrown in your suspend function.

### Authentication Flow

With a flow which is described in following documents, WebAuthnClient finds credentials, let user to select one (if multiple), and signs the response with it.

- https://www.w3.org/TR/webauthn/#getAssertion
- https://www.w3.org/TR/webauthn/#op-get-assertion

```kotlin
private suspend fun executeAuthentication() {

    val options = PublicKeyCredentialRequestOptions()

    options.challenge        = ByteArrayUtil.fromHex(challenge)
    options.rpId             = relyingParty
    options.userVerification = userVerification

    if (credId.isNotEmpty()) {
        options.addAllowCredential(
            credentialId = ByteArrayUtil.fromHex(credId),
            transports   = mutableListOf(AuthenticatorTransport.Internal))
    }

    try {

        val assertion = client.get(options)

        // send parameters to your server
        //assertion.id
        //assertion.rawId
        //assertion.response.authenticatorData
        //assertion.response.signature
        //assertion.response.userHandle
        //assertion.response.clientDataJSON

    } catch (e: Exception) {
        // error handling
    }

}
```

## Features

### Not Implemented yet

- Token Binding
- Extensions
- BLE Authenticator
- BLE Roaming Service
- SafetyNet Attestation

### Key Algorithm Support

- ES256

### Resident Key

InternalAuthenticator forces to use resident-key.

### Attestation

Currently, this library supports only self-attestation.

## See Also

- https://www.w3.org/TR/webauthn/
- https://fidoalliance.org/specs/fido-v2.0-rd-20170927/fido-client-to-authenticator-protocol-v2.0-rd-20170927.html

## License

MIT-LICENSE

## Author

Lyo Kato <lyo.kato at gmail.com>

