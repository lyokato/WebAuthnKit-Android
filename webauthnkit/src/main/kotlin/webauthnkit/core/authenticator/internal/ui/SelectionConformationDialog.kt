package webauthnkit.core.authenticator.internal.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.FragmentActivity
import webauthnkit.core.R
import webauthnkit.core.authenticator.internal.PublicKeyCredentialSource
import webauthnkit.core.util.WAKLogger

@ExperimentalUnsignedTypes
interface SelectionConfirmationDialogInterface {
    fun onSelect(source: PublicKeyCredentialSource)
    fun onCancel()
}

@ExperimentalUnsignedTypes
class SelectionConformationDialog(
    private val activity: FragmentActivity,
    private val sources:  List<PublicKeyCredentialSource>,
    private val dialog: Dialog = Dialog(activity)
) {

    companion object {
       val TAG = SelectionConformationDialog::class.simpleName
    }

    var listener: SelectionConfirmationDialogInterface? = null

    fun show() {

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        )

        dialog.setContentView(R.layout.webauthn_selection_conformation_dialog)

        val spinner = dialog.findViewById<Spinner>(R.id.webauthn_selection_sources)
        val sourceTitles = sources.map { it.otherUI }
        val sourcesAdapter = ArrayAdapter<String>(activity, R.layout.webauthn_selection_source_item, sourceTitles)
        spinner.adapter = sourcesAdapter

        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.findViewById<Button>(R.id.webauthn_selection_confirmation_cancel_button).setOnClickListener {
            WAKLogger.d(RegistrationConfirmationDialog.TAG, "cancel clicked")
            dialog.dismiss()
            listener?.onCancel()
        }

        dialog.findViewById<Button>(R.id.webauthn_selection_confirmation_ok_button).setOnClickListener {
            WAKLogger.d(RegistrationConfirmationDialog.TAG, "select clicked")
            val selected = sources[spinner.selectedItemPosition]
            WAKLogger.w(TAG, "SELECTED ${selected.otherUI}")
            dialog.dismiss()
            listener?.onSelect(selected)
        }

        dialog.show()

    }

}