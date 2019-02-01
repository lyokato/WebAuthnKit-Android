package webauthnkit.example

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ImplicitReflectionSerializer
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import webauthnkit.core.util.WAKLogger

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WAKLogger.available = true

        verticalLayout {

            padding = dip(10)

            button("Registration") {
                textSize = 24f

                onClick {
                    goToRegistrationActivity()
                }

            }

            button("Authentication") {
                textSize = 24f

                onClick {
                    goToAuthenticationActivity()
                }
            }

        }
    }

    private fun goToRegistrationActivity() {
        var intent = Intent(this, RegistrationActivity::class.java)
        startActivity(intent)
    }

    private fun goToAuthenticationActivity() {
        var intent = Intent(this, AuthenticationActivity::class.java)
        startActivity(intent)
    }
}
