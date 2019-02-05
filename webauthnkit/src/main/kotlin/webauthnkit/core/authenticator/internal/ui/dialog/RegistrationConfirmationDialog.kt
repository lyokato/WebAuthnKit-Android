package webauthnkit.core.authenticator.internal.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.format.DateFormat
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import webauthnkit.core.data.*
import webauthnkit.core.R
import webauthnkit.core.authenticator.internal.ui.UserConsentUIConfig
import webauthnkit.core.util.WAKLogger
import java.util.*

interface RegistrationConfirmationDialogListener {
    fun onCreate(keyName: String)
    fun onCancel()
}

interface RegistrationConfirmationDialog {
    fun show(
        activity:   FragmentActivity,
        rpEntity:   PublicKeyCredentialRpEntity,
        userEntity: PublicKeyCredentialUserEntity,
        listener: RegistrationConfirmationDialogListener
    )
}

class DefaultRegistrationConfirmationDialog(
    private val config: UserConsentUIConfig
): RegistrationConfirmationDialog {

    companion object {
        val TAG = DefaultRegistrationConfirmationDialog::class.simpleName
    }

    override fun show(
        activity:   FragmentActivity,
        rpEntity:   PublicKeyCredentialRpEntity,
        userEntity: PublicKeyCredentialUserEntity,
        listener: RegistrationConfirmationDialogListener
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

        dialog.setContentView(R.layout.webauthn_registration_conformation_dialog)

        dialog.findViewById<TextView>(R.id.webauthn_registration_confirmation_title).text =
            config.registrationDialogTitle

        dialog.findViewById<TextView>(R.id.webauthn_registration_username).text =
            userEntity.displayName

        val defaultKeyName = getDefaultKeyName(userEntity.name)

        val keyNameField = dialog.findViewById<EditText>(R.id.webauthn_registration_key_name)
        keyNameField.setText(defaultKeyName)

        val rpName = "[ ${rpEntity.name} ]"
        dialog.findViewById<TextView>(R.id.webauthn_registration_rp).text = rpName

        val userIconView = dialog.findViewById<ImageView>(R.id.webauthn_registration_user_icon)

        userEntity.icon?.let {

            val radius = activity.resources.getDimensionPixelSize(R.dimen.user_icon_radius)

            val option= RequestOptions().let {

                it.fitCenter()
                it.transform(MultiTransformation(CenterCrop(), RoundedCorners(radius)))

            }
            Glide.with(activity)
                .load(userEntity.icon)
                .apply(option)
                .into(userIconView)
        }


        val rpIconView = dialog.findViewById<ImageView>(R.id.webauthn_registration_rp_icon)

        rpEntity.icon?.let {

            val radius = activity.resources.getDimensionPixelSize(R.dimen.rp_icon_radius)

            val option= RequestOptions().let {

                it.fitCenter()
                it.transform(MultiTransformation(CenterCrop(), RoundedCorners(radius)))
            }

            Glide.with(activity)
                .load(rpEntity.icon)
                .apply(option)
                .into(rpIconView)
        }

        dialog.findViewById<Button>(R.id.webauthn_registration_confirmation_cancel_button).text =
            config.registrationDialogCancelButtonText

        dialog.findViewById<Button>(R.id.webauthn_registration_confirmation_cancel_button).setOnClickListener {
            WAKLogger.d(TAG, "cancel clicked")
            dialog.dismiss()
            listener.onCancel()
        }

        dialog.findViewById<Button>(R.id.webauthn_registration_confirmation_ok_button).text =
            config.registrationDialogCreateButtonText

        dialog.findViewById<Button>(R.id.webauthn_registration_confirmation_ok_button).setOnClickListener {
            WAKLogger.d(TAG, "create clicked")
            val keyName = keyNameField.text.toString()
            dialog.dismiss()
            if (keyName.isEmpty()) {
                listener.onCreate(getDefaultKeyName(userEntity.name))
            } else {
                listener.onCreate(keyName)
            }
        }

        dialog.show()
    }

    private fun getDefaultKeyName(username: String): String {
        val date = DateFormat.format("yyyyMMdd", Calendar.getInstance())
        return "$username($date)"
    }

}