package webauthnkit.example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.jetbrains.anko.button
import org.jetbrains.anko.verticalLayout


class RegistrationResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val credId = intent.getStringExtra("CRED_ID")
        val credRaw = intent.getStringExtra("CRED_RAW")
        val clientJSON = intent.getStringExtra("CLIENT_JSON")
        val attestation = intent.getStringExtra("ATTESTATION")

        verticalLayout {


            button("CLOSE") {
                onCloseButtonClicked()
            }
        }
    }


    private fun onCloseButtonClicked() {

    }

}