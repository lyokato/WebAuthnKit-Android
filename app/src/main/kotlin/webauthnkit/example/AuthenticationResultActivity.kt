package webauthnkit.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.jetbrains.anko.button
import org.jetbrains.anko.verticalLayout

class AuthenticationResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            button("CLOSE") {
                onCloseButtonClicked()
            }
        }
    }

    private fun onCloseButtonClicked() {

    }

}