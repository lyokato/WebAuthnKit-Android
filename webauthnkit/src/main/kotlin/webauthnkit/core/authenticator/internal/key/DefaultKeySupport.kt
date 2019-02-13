package webauthnkit.core.authenticator.internal.key

import android.annotation.TargetApi
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import webauthnkit.core.util.WAKLogger
import webauthnkit.core.authenticator.*
import webauthnkit.core.util.ByteArrayUtil
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.*
import android.security.keystore.KeyInfo
import webauthnkit.core.error.InvalidStateException
import java.security.cert.X509Certificate

@TargetApi(Build.VERSION_CODES.M)
@ExperimentalUnsignedTypes
class DefaultKeySupport(
    override val alg: Int
): KeySupport {

    companion object {
        val TAG = DefaultKeySupport::class.simpleName
    }

    override fun createKeyPair(alias: String, clientDataHash: ByteArray): COSEKey? {

        try {
            val generator =
                KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,
                    KeyStoreType.Android
                )

            generator.initialize(createGenParameterSpec(alias, clientDataHash))

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
            WAKLogger.w(TAG, "failed to create key pair: " + e.localizedMessage)
            return null
        }

    }

    private fun createGenParameterSpec(alias: String, clientDataHash: ByteArray): KeyGenParameterSpec {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createGenParameterSpecN(alias, clientDataHash)
        } else {
            createGenParameterSpecM(alias)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun createGenParameterSpecN(alias: String, clientDataHash: ByteArray): KeyGenParameterSpec {
        return KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec(CurveType.SECP256r1))
            .setDigests(
                KeyProperties.DIGEST_SHA256)
            //.setIsStrongBoxBacked(true)
            //.setUnlockedDeviceRequired(true)
            .setUserAuthenticationRequired(false)
            .setUserAuthenticationValidityDurationSeconds(5 * 60)
            .setAttestationChallenge(clientDataHash)
            .build()
    }

    private fun createGenParameterSpecM(alias: String): KeyGenParameterSpec {
        return KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec(CurveType.SECP256r1))
            .setDigests(
                KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            //.setUserAuthenticationValidityDurationSeconds(5 * 60)
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

    private fun useSecureHardware(alias: String): Boolean {
        val keyStore = KeyStore.getInstance(KeyStoreType.Android)
        keyStore.load(null)
        val key= keyStore.getKey(alias, null)
        val factory = KeyFactory.getInstance(key.algorithm,
            KeyStoreType.Android
        )
        val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
        return keyInfo.isInsideSecureHardware
        // && keyInfo.isUserAuthenticationRequirementEnforcedBySecureHardware
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

        if (useSecureHardware(alias)) {

            WAKLogger.d(
                TAG,
                "this android device supports secure-hardware, "
                        + "so, use 'attestation-key' format")

            val keyStore = KeyStore.getInstance(KeyStoreType.Android)
            keyStore.load(null)

            val certs = keyStore.getCertificateChain(alias)

            val x5c = ArrayList<ByteArray>()
            for (cert in certs) {
                x5c.add((cert as X509Certificate).encoded)
            }

            attStmt["x5c"] = x5c

            return AttestationObject(
                fmt      = AttestationFormatType.AndroidKey,
                authData = authenticatorData,
                attStmt  = attStmt
            )

        } else {

            WAKLogger.d(
                TAG,
                "this android device doesn't support secure-hardware, "
                        + "so, build self attestation")

            return AttestationObject(
                fmt      = AttestationFormatType.Packed,
                authData = authenticatorData,
                attStmt  = attStmt
            )
        }

    }
}

