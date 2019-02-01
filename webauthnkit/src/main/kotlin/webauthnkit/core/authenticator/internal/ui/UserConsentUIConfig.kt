package webauthnkit.core.authenticator.internal.ui


class UserConsentUIConfig {

    var alwaysShowKeySelection: Boolean = false

    var messageKeyguardTitle       = "Device Authentication"
    var messageKeyguardDescription = "Authenticate to handle account information"
    var messageKeyguardNotSetError = "Currently, keyguard is not set"

    var messageVerificationNotSupported = "Verification is not supported on Android5"

    var registrationDialogTitle = "New Login Key"

    var registrationDialogCreateButtonText = "CREATE"
    var registrationDialogCancelButtonText = "CANCEL"

    var selectionDialogTitle = "Select Key"

    var selectionDialogSelectButtonText = "SELECT"
    var selectionDialogCancelButtonText = "CANCEL"

    var errorDialogTitle = "Verification Failed"
    var errorDialogOKButtonTet = "OK"
}