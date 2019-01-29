package webauthnkit.example

import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.WAKLogger

class AuthenticationResultActivity : AppCompatActivity() {

    companion object {
        private val TAG = AuthenticationResultActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val credId = intent.getStringExtra("CRED_ID")
        val credRaw = intent.getStringExtra("CRED_RAW")
        val clientJSON = intent.getStringExtra("CLIENT_JSON")
        val authenticatorData = intent.getStringExtra("AUTHENTICATOR_DATA")
        val signature = intent.getStringExtra("SIGNATURE")
        val userHandle = intent.getStringExtra("USER_HANDLE")

        val jsonBase64 = ByteArrayUtil.encodeBase64URL(clientJSON.toByteArray())
        WAKLogger.d(TAG, "CRED_ID:" + credId)
        WAKLogger.d(TAG, "CRED_RAW:" + credRaw)
        WAKLogger.d(TAG, "CLIENT_JSON:" + jsonBase64)
        WAKLogger.d(TAG, "AUTHENTICATOR_DATA:" + authenticatorData)
        WAKLogger.d(TAG, "signature:" + signature)
        WAKLogger.d(TAG, "userHandle:" + userHandle)

        verticalLayout {

            padding = dip(10)

            textView {
                text = "Raw Id"
            }

            val rawIdField = editText {
                singleLine = true
            }
            rawIdField.setText(credRaw)

            textView {
                text = "Credential Id (Base64 URL)"
            }

            val credIdField = editText {
                singleLine = true
            }
            credIdField.setText(credId)

            textView {
                text = "Client Data JSON"
            }

            val clientDataField = editText {
                inputType =  InputType.TYPE_TEXT_FLAG_MULTI_LINE
                height = dip(100)
            }
            clientDataField.setText(clientJSON)

            textView {
                text = "Authenticator Data (Base64 URL)"
            }

            val authenticatorDataField = editText {
                inputType =  InputType.TYPE_TEXT_FLAG_MULTI_LINE
                height = dip(100)
            }
            authenticatorDataField.setText(authenticatorData)

            textView {
                text = "Signature (Hex)"
            }

            val signatureField = editText {
                singleLine = true
            }
            signatureField.setText(signature)

            textView {
                text = "User Handle"
            }

            val userHandleField = editText {
                singleLine = true
            }
            userHandleField.setText(userHandle)

            button("CLOSE") {

                onClick {
                    onCloseButtonClicked()
                }
            }
        }
    }

    private fun onCloseButtonClicked() {
        finish()
    }

}