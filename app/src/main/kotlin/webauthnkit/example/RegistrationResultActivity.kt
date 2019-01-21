package webauthnkit.example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.WAKLogger


class RegistrationResultActivity : AppCompatActivity() {

    companion object {
        private val TAG = this::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val credId = intent.getStringExtra("CRED_ID")
        val credRaw = intent.getStringExtra("CRED_RAW")
        val clientJSON = intent.getStringExtra("CLIENT_JSON")
        val attestation = intent.getStringExtra("ATTESTATION")

        val jsonBase64 = ByteArrayUtil.encodeBase64URL(clientJSON.toByteArray())
        WAKLogger.d(TAG, "CRED_ID:" + credId)
        WAKLogger.d(TAG, "CRED_RAW:" + credRaw)
        WAKLogger.d(TAG, "CLIENT_JSON:" + jsonBase64)
        WAKLogger.d(TAG, "ATTESTATION:" + attestation)

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
                text = "Attestation Object (Base64 URL)"
            }

            val attestationField = editText {
                inputType =  InputType.TYPE_TEXT_FLAG_MULTI_LINE
                height = dip(100)
            }
            attestationField.setText(attestation)


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