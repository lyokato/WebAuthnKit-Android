package webauthnkit.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity;
import org.jetbrains.anko.*

class AuthenticationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            button("Authenticate") {

            }
        }
    }

}
