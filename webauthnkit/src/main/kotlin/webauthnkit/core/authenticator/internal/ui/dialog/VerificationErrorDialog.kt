package webauthnkit.core.authenticator.internal.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import webauthnkit.core.R
import webauthnkit.core.authenticator.internal.ui.UserConsentUIConfig
import webauthnkit.core.util.WAKLogger

interface VerificationErrorDialogListener {
    fun onComplete()
}

class VerificationErrorDialog(
   private val config: UserConsentUIConfig
) {

    companion object {
        val TAG = VerificationErrorDialog::class.simpleName
    }

    fun show(
        activity: FragmentActivity,
        reason:   String,
        listener: VerificationErrorDialogListener
    ) {

        WAKLogger.d(TAG, "show")

        val dialog = Dialog(activity)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        )

        dialog.setContentView(R.layout.webauthn_verification_error_dialog)

        dialog.findViewById<TextView>(R.id.webauthn_verification_error_title).text = config.errorDialogTitle
        dialog.findViewById<TextView>(R.id.webauthn_verification_error_reason).text = reason
        dialog.findViewById<Button>(R.id.webauthn_verification_error_ok_button).text = config.errorDialogOKButtonTet

        dialog.findViewById<Button>(R.id.webauthn_verification_error_ok_button).setOnClickListener {
            WAKLogger.d(TAG, "ok clicked")
            dialog.dismiss()
            listener.onComplete()
        }

        dialog.show()
    }

}