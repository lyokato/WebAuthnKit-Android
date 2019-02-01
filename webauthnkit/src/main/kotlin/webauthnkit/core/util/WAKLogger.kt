package webauthnkit.core.util

import android.util.Log

object WAKLogger {

    val libraryPart = "WebAuthnKit"

    var available: Boolean = false

    fun d(tag: String?, msg: String) {
        if (available) {
            Log.d(libraryPart, wrapMessage(tag!!, msg))
        }
    }

    fun e(tag: String?, msg: String) {
        if (available) {
            Log.e(libraryPart, wrapMessage(tag!!, msg))
        }
    }

    fun i(tag: String?, msg: String) {
        if (available) {
            Log.i(libraryPart, wrapMessage(tag!!, msg))
        }
    }

    fun w(tag: String?, msg: String) {
        if (available) {
            Log.w(libraryPart, wrapMessage(tag!!, msg))
        }
    }

    private fun wrapMessage(klass: String, msg: String): String {
        return "[$klass] $msg"
    }

}