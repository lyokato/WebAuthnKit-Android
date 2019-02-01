package webauthnkit.core.authenticator.internal.key

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.authenticator.*

@ExperimentalUnsignedTypes
class KeySupportChooser(private val context: Context) {

    companion object {
        val TAG = KeySupportChooser::class.simpleName
    }

    fun choose(algorithms: List<Int>): KeySupport? {
        WAKLogger.d(TAG, "choose support module")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            chooseInternal(algorithms)
        } else {
            WAKLogger.d(TAG, "this android version is below M, use legacy version")
            chooseLegacyInternal(algorithms)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun chooseInternal(algorithms: List<Int>): KeySupport? {
        for (alg in algorithms) {
            when (alg) {
                COSEAlgorithmIdentifier.es256 -> {
                    return DefaultKeySupport(alg)
                }
                else -> {
                    WAKLogger.d(TAG, "key support for this algorithm not found")
                }
            }
        }
        WAKLogger.w(TAG, "no proper support module found")
        return null
    }

    private fun chooseLegacyInternal(algs: List<Int>): KeySupport? {
        for (alg in algs) {
            when (alg) {
                COSEAlgorithmIdentifier.es256 -> {
                    return LegacyKeySupport(context, alg)
                }
                else -> {
                    WAKLogger.d(TAG, "key support for this algorithm not found")
                }
            }
        }
        WAKLogger.w(TAG, "no proper support module found")
        return null
    }
}

