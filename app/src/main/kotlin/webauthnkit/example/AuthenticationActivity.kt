package webauthnkit.example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
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
