package webauthnkit.example

import android.view.ViewManager
import com.jaredrummler.materialspinner.MaterialSpinner
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.materialSpinner(theme: Int = 0, init: MaterialSpinner.() -> Unit) =
    ankoView({ MaterialSpinner(it) }, theme, init)