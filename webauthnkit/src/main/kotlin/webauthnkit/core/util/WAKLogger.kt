package webauthnkit.core.util

import android.util.Log

object WAKLogger {

    var available: Boolean = false

    fun d(tag: String?, msg: String) {
        if (available) {
            Log.d(tag, wrapMessage(msg))
        }
    }

    fun e(tag: String?, msg: String) {
        if (available) {
            Log.e(tag, wrapMessage(msg))
        }
    }

    fun i(tag: String?, msg: String) {
        if (available) {
            Log.i(tag, wrapMessage(msg))
        }
    }

    fun w(tag: String?, msg: String) {
        if (available) {
            Log.w(tag, wrapMessage(msg))
        }
    }

    private fun wrapMessage(msg: String): String {
        return "<WebAuthnKit> $msg"
    }

}