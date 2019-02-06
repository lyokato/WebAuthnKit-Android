package webauthnkit.core.authenticator.internal.key

import android.content.Context
import android.security.KeyPairGeneratorSpec
import webauthnkit.core.error.InvalidStateException
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.authenticator.*
import webauthnkit.core.util.ByteArrayUtil
import java.math.BigInteger
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.*
import javax.security.auth.x500.X500Principal

@ExperimentalUnsignedTypes
class LegacyKeySupport(
    private val context: Context,
    override val alg: Int
): KeySupport {

    companion object {
        val TAG = LegacyKeySupport::class.simpleName
    }

    override fun createKeyPair(alias: String, clientDataHash: ByteArray): COSEKey? {
        try {
            val generator =
                KeyPairGenerator.getInstance("EC",
                    KeyStoreType.Android
                )
            generator.initialize(this.createKeyPairSpec(alias))
            val pubKey = generator.generateKeyPair().public as ECPublicKey

            val encoded = pubKey.encoded
            if (encoded.size != 91) {
                throw InvalidStateException("length of ECPublicKey should be 91")
            }

            val x = Arrays.copyOfRange(encoded, 27, 59)
            val y = Arrays.copyOfRange(encoded, 59, 91)

            return COSEKeyEC2(
                alg = alg,
                crv = COSEKeyCurveType.p256,
                x   = x,
                y   = y
            )
        } catch (e: Exception) {
            WAKLogger.w(TAG, "failed to create key pair" + e.localizedMessage)
            return null
        }
    }

    private fun createKeyPairSpec(alias: String): KeyPairGeneratorSpec {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        end.add(Calendar.YEAR, 100)
        return KeyPairGeneratorSpec.Builder(context)
            .setAlgorithmParameterSpec(ECGenParameterSpec(CurveType.SECP256r1))
            .setAlias(alias)
            .setSubject(X500Principal("CN=$alias"))
            .setSerialNumber(BigInteger.valueOf(1000000))
            .setStartDate(start.time)
            .setEndDate(end.time)
            .build()
    }

    override fun sign(alias: String, data: ByteArray): ByteArray? {
        val keyStore = KeyStore.getInstance(KeyStoreType.Android)
        keyStore.load(null)
        val privateKey = keyStore.getKey(alias, null) as PrivateKey
        val signer = Signature.getInstance(SignAlgorithmType.SHA256WithECDSA)
        signer.initSign(privateKey)
        signer.update(data)
        return signer.sign()
    }

    override fun buildAttestationObject(
        alias:             String,
        clientDataHash:    ByteArray,
        authenticatorData: AuthenticatorData
    ): AttestationObject? {

        val authenticatorDataBytes = authenticatorData.toBytes()
        if (authenticatorDataBytes == null) {
            WAKLogger.d(TAG, "failed to build authenticator data")
            return null
        }

        val bytesToBeSigned =
            ByteArrayUtil.merge(authenticatorDataBytes, clientDataHash)

        val sig = this.sign(alias, bytesToBeSigned)
        if (sig == null) {
            WAKLogger.d(TAG, "failed to sign authenticator data")
            return null
        }

        val attStmt = HashMap<String, Any>()
        attStmt["alg"] = alg.toLong()
        attStmt["sig"] = sig

        return AttestationObject(
            fmt      = AttestationFormatType.Packed,
            authData = authenticatorData,
            attStmt  = attStmt
        )
    }
}

