package webauthnkit.core

import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import webauthnkit.core.util.CBORWriter
import webauthnkit.core.util.ByteArrayUtil
import webauthnkit.core.util.CBORReader
import webauthnkit.core.util.WAKLogger
import java.util.*

@ExperimentalUnsignedTypes
class CBORUnitTest {

    @Before fun setup() {
        WAKLogger.available = true
    }

    private fun assertNumber(num: Long, hex: String) {
        val computed = ByteArrayUtil.toHex(CBORWriter().putNumber(num).compute())
        assertEquals(hex, computed)

        val reader = CBORReader(ByteArrayUtil.fromHex(hex))
        assertEquals(num, reader.readNumber())
    }

    private fun assertFloat(num: Float, hex: String) {
        val computed = ByteArrayUtil.toHex(CBORWriter().putFloat(num).compute())
        assertEquals(hex, computed)

        val reader = CBORReader(ByteArrayUtil.fromHex(hex))
        assertEquals(num, reader.readFloat())
    }

    private fun assertDouble(num: Double, hex: String) {
        val computed = ByteArrayUtil.toHex(CBORWriter().putDouble(num).compute())
        assertEquals(hex, computed)

        val reader = CBORReader(ByteArrayUtil.fromHex(hex))
        assertEquals(num, reader.readDouble())
    }

    private fun assertBool(value: Boolean, hex: String) {
        val computed = ByteArrayUtil.toHex(CBORWriter().putBool(value).compute())
        assertEquals(hex, computed)

        val reader = CBORReader(ByteArrayUtil.fromHex(hex))
        assertEquals(value, reader.readBool())
    }

    private fun assertString(value: String, hex: String) {
        val computed = ByteArrayUtil.toHex(CBORWriter().putString(value).compute())
        assertEquals(hex, computed)

        val reader = CBORReader(ByteArrayUtil.fromHex(hex))
        assertEquals(value, reader.readString())
    }

    private fun assertByteString(value: ByteArray, hex: String) {
        val computed = ByteArrayUtil.toHex(CBORWriter().putByteString(value).compute())
        assertEquals(hex, computed)
        val reader = CBORReader(ByteArrayUtil.fromHex(hex))
        val result =  reader.readByteString()!!
        assertNotNull(result)
        assertArrayEquals(value, result)
    }


    @Test
    fun number_isCorrect() {
        assertNumber(num = 0, hex = "00")
        assertNumber(num = 1, hex = "01")
        assertNumber(num = 10, hex = "0a")
        assertNumber(num = 23, hex = "17")
        assertNumber(num = 24, hex = "1818")
        assertNumber(num = 25, hex = "1819")
        assertNumber(num = 100, hex = "1864")
        assertNumber(num = 1000, hex = "1903e8")
        assertNumber(num = 1000000, hex = "1a000f4240")
        assertNumber(num = 1000000000000, hex = "1b000000e8d4a51000")

        assertNumber(num = -1, hex = "20")
        assertNumber(num = -10, hex = "29")
        assertNumber(num = -100, hex = "3863")
        assertNumber(num = -1000, hex = "3903e7")
    }

    @Test
    fun float_isCorrect() {
        assertFloat(num = 100000.0F, hex = "fa47c35000")
        assertFloat(num = 3.4028234663852886e+38F, hex = "fa7f7fffff")
    }

    @Test
    fun double_isCorrect() {
        assertDouble(num = 1.1, hex = "fb3ff199999999999a")
        assertDouble(num = 1.0e+300, hex = "fb7e37e43c8800759c")
        assertDouble(num = -4.1, hex = "fbc010666666666666")
    }

    @Test
    fun string_isCorrect() {
        assertString(value="", hex="60")
        assertString(value="a", hex="6161")
        assertString(value="IETF", hex="6449455446")
        assertString(value="\"\\", hex="62225c")
        assertString(value="${'\u00fc'}", hex="62c3bc")
        assertString(value="${'\u6c34'}", hex="63e6b0b4")
    }

    @Test
    fun byteString_isCorrect() {
        assertByteString(value= byteArrayOf(), hex="40")
        assertByteString(value= byteArrayOf(0x01, 0x02, 0x03, 0x04), hex="4401020304")
    }

    @Test
    fun bool_isCorrect() {
        assertBool(value = false, hex = "f4")
        assertBool(value = true, hex = "f5")
    }

    @Test
    fun null_isCorrect() {
        val computed = ByteArrayUtil.toHex(CBORWriter().putNull().compute())
        assertEquals("f6", computed)
    }

    @Test
    fun array_isCorrect() {
        val list = mutableListOf<Any>()
        val computed1 = ByteArrayUtil.toHex(CBORWriter().putArray(list).compute())
        assertEquals("80", computed1)

        val result1 = CBORReader(ByteArrayUtil.fromHex(computed1)).readArray()
        assertEquals(0, result1!!.count())

        val list2 = mutableListOf<Any>(1L, 2L, 3L)
        val computed2 = ByteArrayUtil.toHex(CBORWriter().putArray(list2).compute())
        assertEquals("83010203", computed2)
        val result2 = CBORReader(ByteArrayUtil.fromHex(computed2)).readArray()
        assertEquals(3, result2!!.count())
        assertEquals(1L, result2[0])
        assertEquals(2L, result2[1])
        assertEquals(3L, result2[2])


        val list3 = mutableListOf<Any>(1L,2L,3L,4L,5L,6L,7L,8L,9L,10L,
            11L,12L,13L,14L,15L,16L,17L,18L,19L,20L,21L,22L,23L,24L,25L)
        val computed3 = ByteArrayUtil.toHex(CBORWriter().putArray(list3).compute())
        assertEquals("98190102030405060708090a0b0c0d0e0f101112131415161718181819", computed3)
        val result3 = CBORReader(ByteArrayUtil.fromHex(computed3)).readArray()
        assertEquals(25, result3!!.count())
        assertEquals(1L, result3[0])
        assertEquals(25L, result3[24])
    }

    @Test
    fun map_isCorrect() {
        val map1 = mutableMapOf<String, Any>()
        val computed = ByteArrayUtil.toHex(CBORWriter().putStringKeyMap(map1).compute())
        assertEquals("a0", computed)
        val result1 = CBORReader(ByteArrayUtil.fromHex(computed)).readStringKeyMap()
        assertEquals(0, result1!!.count())

        val map2 = mutableMapOf<String, Any>()
        map2["a"] = 1L
        val array2 = mutableListOf<Any>(2L, 3L)
        map2["b"] = array2
        val computed2 = ByteArrayUtil.toHex(CBORWriter().putStringKeyMap(map2).compute())
        assertEquals("a26161016162820203", computed2)
        val result2 = CBORReader(ByteArrayUtil.fromHex(computed2)).readStringKeyMap()
        assertEquals(2, result2!!.count())
        assertEquals(1L, result2["a"])

        val map3 = mutableMapOf<Int, Any>()
        map3[1] = 2L
        map3[3] = 4L
        val computed3 = ByteArrayUtil.toHex(CBORWriter().putIntKeyMap(map3).compute())
        assertEquals("a0", computed)
        val result3 = CBORReader(ByteArrayUtil.fromHex(computed3)).readIntKeyMap()
        assertEquals(2, result3!!.count())
        assertEquals(2L, result3[1])
        assertEquals(4L, result3[3])
    }
}