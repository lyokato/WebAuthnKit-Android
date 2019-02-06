package webauthnkit.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import webauthnkit.core.util.WAKLogger

@ExperimentalCoroutinesApi
@ExperimentalUnsignedTypes
class BleAuthenticatorAuthenticationActivity : AppCompatActivity() {

    companion object {
        val TAG = BleAuthenticatorAuthenticationActivity::class.simpleName
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "AUTHENTICATION BLE SERVICE"

        verticalLayout {

            padding = dip(10)

            button("START") {
                textSize = 24f

                onClick {

                }

            }

            button("CLOSE") {
                textSize = 24f

                onClick {

                }
            }

        }
    }

}
