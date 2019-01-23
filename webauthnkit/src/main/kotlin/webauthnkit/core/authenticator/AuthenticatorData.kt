package webauthnkit.core.authenticator

import webauthnkit.core.util.WAKLogger
import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.CBORWriter

object COSEKeyFieldType {
    const val kty:    Int =  1
    const val alg:    Int =  3
    const val crv:    Int = -1
    const val xCoord: Int = -2
    const val yCoord: Int = -3
    const val n:      Int = -1
    const val e:      Int = -2
}

object COSEKeyCurveType {
    const val p256:    Int = 1
    /*
    const val p384:    Int = 2
    const val p521:    Int = 3
    const val x25519:  Int = 4
    const val x448:    Int = 5
    const val ed25519: Int = 6
    const val ed448:   Int = 7
    */
}

object COSEKeyType {
    const val ec2: Int = 2
    const val rsa: Int = 3
}

object COSEAlgorithmIdentifier {
    const val rs256 = -257
    const val es256 =   -7
    /*
    val rs384 = -258
    val rs512 = -259
    val es384 =  -35
    val es512 =  -36
    val ed256 = -260
    val ed512 = -261
    val ps256 =  -37
    */
}

interface COSEKey {
    fun toBytes(): ByteArray?
}

@ExperimentalUnsignedTypes
class COSEKeyEC2(
    val alg: Int,
    val crv: Int,
    val x: ByteArray,
    val y: ByteArray
): COSEKey {

    companion object {
        val TAG = this::class.simpleName
    }

    override fun toBytes(): ByteArray? {
        WAKLogger.w(TAG, "COSE:EC2:toBytes")

        try {
            val map = LinkedHashMap<Int, Any>()

            map[COSEKeyFieldType.kty]    = COSEKeyType.ec2.toLong()
            map[COSEKeyFieldType.alg]    = this.alg.toLong()
            map[COSEKeyFieldType.crv]    = this.crv.toLong()
            map[COSEKeyFieldType.xCoord] = this.x
            map[COSEKeyFieldType.yCoord] = this.y

            WAKLogger.d(TAG, ByteArrayUtil.toHex(this.x))
            WAKLogger.d(TAG, ByteArrayUtil.toHex(this.y))

            return CBORWriter().putIntKeyMap(map).compute()

        } catch (e:Exception) {
            WAKLogger.w(TAG, "failed to build CBOR")
            return null
        }

    }

}

@ExperimentalUnsignedTypes
class COSEKeyRSA(
    val alg: Int,
    val n: ByteArray,
    val e: ByteArray
): COSEKey {

    companion object {
        val TAG = this::class.simpleName
    }

    override fun toBytes(): ByteArray? {
        WAKLogger.w(TAG, "COSE:RSA:toBytes")

        try {
            val map = LinkedHashMap<Int, Any>()

            map[COSEKeyFieldType.kty] = COSEKeyType.rsa.toLong()
            map[COSEKeyFieldType.alg] = this.alg.toLong()
            map[COSEKeyFieldType.n]   = this.n
            map[COSEKeyFieldType.e]   = this.e

            return CBORWriter().putIntKeyMap(map).compute()

        } catch (e:Exception) {
            WAKLogger.w(TAG, "failed to build CBOR")
            return null
        }
    }

}

@ExperimentalUnsignedTypes
class AttestedCredentialData(
    val aaguid:              ByteArray,
    val credentialId:        ByteArray,
    val credentialPublicKey: COSEKey
) {

    companion object {
        val TAG = this::class.simpleName
    }

    fun toBytes(): ByteArray? {
        val pubKeyBytes = credentialPublicKey.toBytes()
        if (pubKeyBytes == null) {
            WAKLogger.w(TAG, "failed to build COSE key")
            return null
        }
        WAKLogger.d(TAG, "PubKey: length - ${pubKeyBytes.size}")
        val credentialIdLength: UInt = credentialId.size.toUInt()
        val size1 = (credentialIdLength and 0x0000_ff00u).shr(8).toByte()
        val size2 = (credentialIdLength and 0x0000_00ffu).toByte()
        val sizeBytes = byteArrayOf(size1, size2)

        var result = ByteArrayUtil.merge(aaguid, sizeBytes)
        result = ByteArrayUtil.merge(result, credentialId)
        result = ByteArrayUtil.merge(result, pubKeyBytes)
        return result
    }
}

@ExperimentalUnsignedTypes
class AuthenticatorDataFlags(
    private val userPresent:               Boolean,
    private val userVerified:              Boolean,
    private val hasAttestedCredentialData: Boolean,
    private val hasExtension:              Boolean
) {

    companion object {
        val TAG = this::class.simpleName
        val upMask: UInt = 0b0000_0001u
        val uvMask: UInt = 0b0000_0100u
        val atMask: UInt = 0b0100_0000u
        val edMask: UInt = 0b1000_0100u

        /*
        fun parse(flags: UInt): AuthenticatorDataFlags {
            val userPresent               = ((flags and upMask) == upMask)
            val userVerified              = ((flags and uvMask) == uvMask)
            val hasAttestedCredentialData = ((flags and atMask) == atMask)
            val hasExtension              = ((flags and edMask) == edMask)
            return AuthenticatorDataFlags(
                userPresent               = userPresent,
                userVerified              = userVerified,
                hasAttestedCredentialData = hasAttestedCredentialData,
                hasExtension              = hasExtension
            )
        }
        */
    }

    fun toByte(): Byte {

        var result: UInt = 0u

        if (userPresent) {
            result = (result or upMask)
        }
        if (userVerified) {
            result = (result or uvMask)
        }
        if (hasAttestedCredentialData) {
            result = (result or atMask)
        }
        if (hasExtension) {
            result = (result or edMask)
        }

        return result.toByte()
    }
}

@ExperimentalUnsignedTypes
class AuthenticatorData(
    private val rpIdHash:               ByteArray,
    private val userPresent:            Boolean,
    private val userVerified:           Boolean,
    private val signCount:              UInt,
            val attestedCredentialData: AttestedCredentialData?,
    private val extensions:             Map<String, Any>
) {

    companion object {
        val TAG = this::class.simpleName
    }

    fun toBytes(): ByteArray? {

        assert(userPresent != userVerified)

        val flags: Byte = AuthenticatorDataFlags(
            userPresent               = userPresent,
            userVerified              = userVerified,
            hasAttestedCredentialData = (attestedCredentialData != null),
            hasExtension              = extensions.isNotEmpty()
        ).toByte()

        val sc1: Byte = (signCount and 0xff00_0000u).shr(24).toByte()
        val sc2: Byte = (signCount and 0x00ff_0000u).shr(16).toByte()
        val sc3: Byte = (signCount and 0x0000_ff00u).shr(8).toByte()
        val sc4: Byte = (signCount and 0x0000_00ffu).toByte()

        var result = ByteArrayUtil.merge(rpIdHash,
            byteArrayOf(flags, sc1, sc2, sc3, sc4))

        if (attestedCredentialData != null) {
            val attestedCredentialDataBytes = attestedCredentialData.toBytes()
            if (attestedCredentialDataBytes == null) {
                WAKLogger.d(TAG, "failed to build attestedCredentialData")
                return null
            }
            result = ByteArrayUtil.merge(result, attestedCredentialDataBytes)
        }

        if (extensions.isNotEmpty()) {
            // TODO extensions not supported currently
            //result = ByteArrayUtil.merge(result, extensions.toCBORMap())
        }

        return result
    }

}