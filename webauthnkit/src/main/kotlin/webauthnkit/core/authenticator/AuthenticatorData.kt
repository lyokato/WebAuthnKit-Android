package webauthnkit.core.authenticator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import webauthnkit.core.util.WKLogger
import webauthnkit.core.util.ByteArrayUtil

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

@ExperimentalUnsignedTypes
interface COSEKey {
    fun toBytes(): UByteArray?
}

@ExperimentalUnsignedTypes
class COSEKeyEC2(
    val alg: Int,
    val crv: Int,
    val x: UByteArray,
    val y: UByteArray
): COSEKey {

    companion object {
        val TAG = this::class.simpleName
    }

    override fun toBytes(): UByteArray? {

        try {
            val map = LinkedHashMap<Int, Any>()

            map[COSEKeyFieldType.kty]    = COSEKeyType.ec2
            map[COSEKeyFieldType.alg]    = this.alg
            map[COSEKeyFieldType.crv]    = this.crv
            map[COSEKeyFieldType.xCoord] = this.x
            map[COSEKeyFieldType.yCoord] = this.y

            return ObjectMapper(CBORFactory())
                .writeValueAsBytes(map)
                .toUByteArray()

        } catch (e:Exception) {
            WKLogger.w(TAG, "failed to build CBOR")
            return null
        }

    }

}

@ExperimentalUnsignedTypes
class COSEKeyRSA(
    val alg: Int,
    val n: UByteArray,
    val e: UByteArray
): COSEKey {

    companion object {
        val TAG = this::class.simpleName
    }

    override fun toBytes(): UByteArray? {

        try {
            val map = LinkedHashMap<Int, Any>()

            map[COSEKeyFieldType.kty] = COSEKeyType.rsa
            map[COSEKeyFieldType.alg] = this.alg
            map[COSEKeyFieldType.n]   = this.n
            map[COSEKeyFieldType.e]   = this.e

            return ObjectMapper(CBORFactory())
                .writeValueAsBytes(map)
                .toUByteArray()

        } catch (e:Exception) {
            WKLogger.w(TAG, "failed to build CBOR")
            return null
        }
    }

}

@ExperimentalUnsignedTypes
class AttestedCredentialData(
    val aaguid:              UByteArray,
    val credentialId:        UByteArray,
    val credentialPublicKey: COSEKey
) {

    companion object {
        val TAG = this::class.simpleName
    }

    fun toBytes(): UByteArray? {
        val pubKeyBytes = credentialPublicKey.toBytes()
        if (pubKeyBytes == null) {
            WKLogger.w(TAG, "failed to build COSE key")
            return null
        }
        val credentialIdLength: UInt = credentialId.size.toUInt()
        val size1 = (credentialIdLength and 0x0000_ff00u).shr(8).toUByte()
        val size2 = (credentialIdLength and 0x0000_00ffu).toUByte()
        val sizeBytes = ubyteArrayOf(size1, size2)

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

    fun toByte(): UByte {

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

        return result.toUByte()
    }
}

@ExperimentalUnsignedTypes
class AuthenticatorData(
    private val rpIdHash:               UByteArray,
    private val userPresent:            Boolean,
    private val userVerified:           Boolean,
    private val signCount:              UInt,
            val attestedCredentialData: AttestedCredentialData?,
    private val extensions:             Map<String, Any>
) {

    companion object {
        val TAG = this::class.simpleName
    }

    fun toBytes(): UByteArray? {

        val flags: UByte = AuthenticatorDataFlags(
            userPresent               = userPresent,
            userVerified              = userVerified,
            hasAttestedCredentialData = (attestedCredentialData != null),
            hasExtension              = extensions.isNotEmpty()
        ).toByte()

        val sc1: UByte = (signCount and 0xff00_0000u).shr(24).toUByte()
        val sc2: UByte = (signCount and 0x00ff_0000u).shr(16).toUByte()
        val sc3: UByte = (signCount and 0x00ff_0000u).shr(16).toUByte()
        val sc4: UByte = (signCount and 0x00ff_0000u).shr(16).toUByte()

        var result = ByteArrayUtil.merge(rpIdHash,
            ubyteArrayOf(flags, sc1, sc2, sc3, sc4))

        if (attestedCredentialData != null) {
            val attestedCredentialDataBytes = attestedCredentialData.toBytes()
            if (attestedCredentialDataBytes == null) {
                WKLogger.d(TAG, "failed to build attestedCredentialData")
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