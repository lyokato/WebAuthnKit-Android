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
import webauthnkit.core.util.WAKLogger

interface KeyguardFallbackDialogListener {
    fun onRequest()
    fun onCancel()
}

class KeyguardFallbackDialog(){

    companion object {
        val TAG = KeyguardFallbackDialog::class.simpleName
    }

    fun show(
        activity: FragmentActivity,
        reason:   String,
        listener: KeyguardFallbackDialogListener
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

        dialog.setContentView(R.layout.webauthn_keyguard_fallback_dialog)

        dialog.findViewById<TextView>(R.id.webauthn_keyguard_fallback_reason).text = reason

        dialog.findViewById<Button>(R.id.webauthn_keyguard_fallback_cancel_button).setOnClickListener {
            WAKLogger.d(TAG, "cancel clicked")
            dialog.dismiss()
            listener.onCancel()
        }

        dialog.findViewById<Button>(R.id.webauthn_keyguard_fallback_ok_button).setOnClickListener {
            WAKLogger.d(TAG, "request clicked")
            dialog.dismiss()
            listener.onRequest()
        }

        dialog.show()
    }

}