package webauthnkit.core.util

import android.util.Log

object WAKLogger {

    var available: Boolean = false

    fun d(tag: String?, msg: String) {
        if (available) {
            Log.d(tag, msg)
        }
    }

    fun e(tag: String?, msg: String) {
        if (available) {
            Log.e(tag, msg)
        }
    }

    fun i(tag: String?, msg: String) {
        if (available) {
            Log.i(tag, msg)
        }
    }

    fun w(tag: String?, msg: String) {
        if (available) {
            Log.w(tag, msg)
        }
    }

}