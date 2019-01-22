package webauthnkit.core.util

import java.nio.ByteBuffer
import java.util.*

// TODO better performance

@ExperimentalUnsignedTypes
object CBORBits {

    val falseBits: UByte
        get() = 0xf4.toUByte()

    val trueBits: UByte
        get() = 0xf5.toUByte()

    val nullBits: UByte
        get() = 0xf6.toUByte()

    val headerPart: UByte
        get() = 0b11100000.toUByte()

    val valuePart: UByte
        get() = 0b00011111.toUByte()

    val stringHeader: UByte
        get() = 0b01100000.toUByte()

    val bytesHeader: UByte
        get() = 0b01000000.toUByte()

    val negativeHeader: UByte
        get() = 0b00100000.toUByte()

    val floatBits: UByte
        get() = 0xfa.toUByte()

    val doubleBits: UByte
        get() = 0xfb.toUByte()

    val arrayHeader: UByte
        get() = 0x80.toUByte()

    val mapHeader: UByte
        get() = 0xa0.toUByte()

    val indefiniteArrayBits: UByte
        get() = 0x9f.toUByte()

    val indefiniteMapBits: UByte
        get() = 0xbf.toUByte()

    val breakBits: UByte
        get() = 0xff.toUByte()

}


@ExperimentalUnsignedTypes
class CBORReader(val bytes: UByteArray) {

    val size = bytes.size
    var cursor = 0

    companion object {
        private val TAG = this::class.simpleName
    }

    fun getReadSize(): Int {
        return cursor
    }

    fun getRestSize(): Int {
        return (size - cursor)
    }

    fun nextByte(): UByte? {
        return if (this.cursor < this.size) {
            this.bytes[this.cursor]
        } else {
            WAKLogger.d(TAG, "no enough size")
            null
        }
    }

    fun replaceNextByte(value: UByte) {
        this.bytes[this.cursor] = value
    }

    private fun readByte(): UByte? {

        return if (this.cursor < this.size) {

            val b = this.bytes[this.cursor]
            this.cursor = this.cursor + 1

            return b

        } else {
            WAKLogger.d(TAG, "no enough size")
            null
        }
    }

    private fun readBytes(size: Int): UByteArray? {

        return if ((this.cursor + size - 1) < this.size) {

            val b = Arrays.copyOfRange(this.bytes.toByteArray(),
                this.cursor, this.cursor + size)
            this.cursor = this.cursor + size

            return b.toUByteArray()

        } else {
            WAKLogger.d(TAG, "no enough size")
            null
        }
    }

    fun readFloat(): Float? {
        val b1 = this.readBytes(4) ?: return null
        return ByteBuffer.wrap(b1.toByteArray()).float
    }

    fun readDouble(): Double? {
        val b1 = this.readBytes(8) ?: return null
        return ByteBuffer.wrap(b1.toByteArray()).double
    }

    fun readByteString(): UByteArray? {

        val b1 = this.nextByte() ?: return null

        if ((b1 and CBORBits.headerPart) != CBORBits.bytesHeader) {
            WAKLogger.d(TAG, "Invalid 'bytes' format")
            return null
        }

        this.replaceNextByte(b1 and CBORBits.valuePart)

        val len = this.readNumber() ?: return null
        if (len == 0L) {
           return ubyteArrayOf()
        }

        return this.readBytes(len.toInt())
    }

    fun readString(): String? {

        val b1 = this.nextByte() ?: return null

        if ((b1 and CBORBits.headerPart) != CBORBits.stringHeader) {
            WAKLogger.d(TAG, "Invalid 'string' format")
            return null
        }

        this.replaceNextByte(b1 and CBORBits.valuePart)

        val len = this.readNumber() ?: return null
        if (len == 0L) {
            return ""
        }

        val b2 = this.readBytes(len.toInt()) ?: return null

        return String(
            bytes   = b2.toByteArray(),
            charset = Charsets.UTF_8
        )
    }

    fun readBool(): Boolean? {

        val b1 = readByte() ?: return false

        return when (b1) {

            CBORBits.falseBits -> {
                false
            }

            CBORBits.trueBits -> {
                true
            }

            else -> {
                null
            }

        }
    }

    fun readNull(): Boolean {
        val b1 = readByte() ?: return false
        return (b1 and CBORBits.nullBits) == CBORBits.nullBits
    }

    fun readAny(): Any? {

        val v1 = nextByte() ?: return null
        val v1i = v1.toInt()

        when {

            v1i in 0..27 -> {
                // positive number
                return this.readNumber()
            }

            v1i in 32..59 -> {
                // negative number
                return this.readNumber()
            }

            v1 == CBORBits.trueBits -> {
                return true
            }

            v1 == CBORBits.falseBits -> {
                return false
            }

            v1 == CBORBits.nullBits -> {
                // FIXME
                return true
            }

            v1 == CBORBits.floatBits -> {
                return this.readFloat()
            }

            v1 == CBORBits.doubleBits -> {
                return this.readDouble()
            }

            (v1 and CBORBits.headerPart) == CBORBits.stringHeader -> {
                return this.readString()
            }

            (v1 and CBORBits.headerPart) == CBORBits.bytesHeader -> {
                return this.readByteString()
            }

            (v1 and CBORBits.headerPart) == CBORBits.arrayHeader -> {
                return this.readArray()
            }

            (v1 and CBORBits.headerPart) == CBORBits.mapHeader -> {
                // currently, support nested-map only when its key is string
                return this.readStringKeyMap()
            }

            else -> {
                WAKLogger.d(TAG, "Unsupported value type for 'Any'")
                return null
            }
        }

    }

    fun readIntKeyMap(): Map<Long, Any>? {

        val b1 = nextByte() ?: return null

        if ((b1 and CBORBits.headerPart) != CBORBits.mapHeader) {
            WAKLogger.d(TAG, "Invalid 'map' format")
            return null
        }

        this.replaceNextByte(b1 and CBORBits.valuePart)

        val count = this.readNumber()?.toInt() ?: return null

        var results = mutableMapOf<Long, Any>()

        if (count == 0) {
            return results
        }

        val max = count - 1
        for (i in 0..max) {
            val key = this.readNumber() ?: return null
            val result = this.readAny() ?: return null
            results[key] = result
        }

        return results
    }

    fun readStringKeyMap(): Map<String, Any>? {

        val b1 = nextByte() ?: return null

        if ((b1 and CBORBits.headerPart) != CBORBits.mapHeader) {
            WAKLogger.d(TAG, "Invalid 'map' format")
            return null
        }

        this.replaceNextByte(b1 and CBORBits.valuePart)

        val count = this.readNumber()?.toInt() ?: return null

        var results = mutableMapOf<String, Any>()

        if (count == 0) {
            return results
        }

        val max = count - 1
        for (i in 0..max) {
            val key = this.readString() ?: return null
            val result = this.readAny() ?: return null
            results[key] = result
        }

        return results
    }

    fun readArray(): List<Any>? {

        val b1 = nextByte() ?: return null

        if ((b1 and CBORBits.headerPart) != CBORBits.arrayHeader) {
            WAKLogger.d(TAG, "Invalid 'array' format")
            return null
        }

        this.replaceNextByte(b1 and CBORBits.valuePart)

        val count = this.readNumber() ?: return null

        var results = mutableListOf<Any>()

        if (count == 0L) {
            return results
        }

        val max = count.toInt() - 1
        for (i in 0..max) {
            val result = this.readAny() ?: return null
            results.add(result)
        }

        return results
    }

    fun readNumber(): Long? {

        val b1 = this.readByte() ?: return null

        val value = (b1 and CBORBits.valuePart).toInt()

        val isNegative = (b1 and CBORBits.headerPart) == CBORBits.negativeHeader

        val bytesToRead = when (value) {

            in 0..23 -> {
                0
            }

            24 -> {
                1
            }

            25 -> {
                2
            }

            26 -> {
                4
            }

            27 -> {
                8
            }

            else -> {
                WAKLogger.d(TAG, "Invalid 'number' format")
                return null
            }

        }

        if (bytesToRead == 0) {
            return if (isNegative) {
                ((value + 1) * -1).toLong()
            } else {
                value.toLong()
            }
        }

        val b2 = this.readBytes(bytesToRead) ?: return null

        val result = when (bytesToRead) {
            1 -> {
                b2[0].toLong()
            }
            2 -> {
                ByteBuffer.wrap(b2.toByteArray()).short.toLong()
            }
            4 -> {
                ByteBuffer.wrap(b2.toByteArray()).int.toLong()
            }
            8 -> {
                ByteBuffer.wrap(b2.toByteArray()).long
            }
            else -> {
                WAKLogger.d(TAG, "Invalid 'number' format")
                return null
            }
        }

        return if (isNegative) {
            -(result + 1)
        } else {
            result
        }

    }
}

@ExperimentalUnsignedTypes
class CBORWriter() {

    private var result = mutableListOf<Byte>()

    fun putArray(values: List<Any>): CBORWriter {
        return this
    }

    fun putStringKeyMap(values: Map<String, Any>): CBORWriter {
        return this
    }

    fun putIntKeyMap(values: Map<Long, Any>): CBORWriter {
        return this
    }

    fun startArray(): CBORWriter {
        this.result.add(CBORBits.indefiniteArrayBits.toByte())
        return this
    }

    fun startMap(): CBORWriter {
        this.result.add(CBORBits.indefiniteMapBits.toByte())
        return this
    }

    fun end(): CBORWriter {
        this.result.add(CBORBits.breakBits.toByte())
        return this
    }

    fun putString(value: String): CBORWriter {
        return this
    }

    fun putByteString(value: ByteArray): CBORWriter {
        return this
    }

    fun putFloat(value: Float): CBORWriter {
        return this
    }

    fun putDouble(value: Double): CBORWriter {
        return this
    }

    fun putNull(): CBORWriter {
        this.result.add(CBORBits.nullBits.toByte())
        return this
    }

    fun putBool(value: Boolean): CBORWriter {
        if (value) {
            this.result.add(CBORBits.trueBits.toByte())
        } else {
            this.result.add(CBORBits.falseBits.toByte())
        }
        return this
    }

    private fun composeNegative(value: Long): List<Byte> {
        

    }

    private fun composePositive(value: Long): List<Byte> {

    }

    fun compute(): ByteArray {
        return result.toByteArray()
    }
}

