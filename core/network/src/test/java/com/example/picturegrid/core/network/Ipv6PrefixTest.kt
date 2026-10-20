package com.example.picturegrid.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверки разбора и сравнения IPv6-префиксов.
 *
 * Разбор выполняется без обращения к DNS, поэтому тесты не зависят от
 * системного резолвера.
 */
class Ipv6PrefixTest {

    @Test
    fun `prefix length and bytes are taken from the same text`() {
        val prefix = Ipv6Prefix.parse("64:ff9b:1::/48")

        assertEquals(48, prefix.lengthBits)
        assertEquals("64:ff9b:1::/48", prefix.toString())
        assertTrue(prefix.matches(bytes(0x0064, 0xFF9B, 0x0001, 0x0000, 0x0000, 0x0000, 0x7F00, 0x0001)))
        assertFalse(prefix.matches(bytes(0x0064, 0xFF9B, 0x0002, 0x0000, 0x0000, 0x0000, 0x7F00, 0x0001)))
    }

    @Test
    fun `compressed and expanded forms describe the same prefix`() {
        val compressed = Ipv6Prefix.parse("2001:db8::/32")
        val expanded = Ipv6Prefix.parse("2001:0db8:0000:0000:0000:0000:0000:0000/32")

        assertEquals(compressed.lengthBits, expanded.lengthBits)
        assertTrue(compressed.matches(address(0x2001, 0x0DB8, 0, 0, 0, 0, 0, 0)))
        assertTrue(expanded.matches(address(0x2001, 0x0DB8, 0, 0, 0, 0, 0, 0)))
    }

    @Test
    fun `mask is applied to the first partially significant byte`() {
        val prefix = Ipv6Prefix.parse("2001::/23")

        assertTrue(prefix.matches(bytes(0x2001, 0x0000, 0x0000, 0, 0, 0, 0, 1)))
        assertTrue(prefix.matches(bytes(0x2001, 0x01FF, 0x0000, 0, 0, 0, 0, 1)))
        assertFalse(prefix.matches(bytes(0x2001, 0x0200, 0x0000, 0, 0, 0, 0, 1)))
        assertFalse(prefix.matches(bytes(0x2001, 0x4860, 0x0000, 0, 0, 0, 0, 1)))
    }

    @Test
    fun `seven bit prefix covers fc00 and fd00 but not fe00`() {
        val prefix = Ipv6Prefix.parse("fc00::/7")

        assertTrue(prefix.matches(bytes(0xFC00, 0, 0, 0, 0, 0, 0, 1)))
        assertTrue(prefix.matches(bytes(0xFDFF, 0, 0, 0, 0, 0, 0, 1)))
        assertFalse(prefix.matches(bytes(0xFE00, 0, 0, 0, 0, 0, 0, 1)))
    }

    @Test
    fun `ten bit prefix covers fe80 and febf but not fec0`() {
        val prefix = Ipv6Prefix.parse("fe80::/10")

        assertTrue(prefix.matches(bytes(0xFE80, 0, 0, 0, 0, 0, 0, 1)))
        assertTrue(prefix.matches(bytes(0xFEBF, 0, 0, 0, 0, 0, 0, 1)))
        assertFalse(prefix.matches(bytes(0xFEC0, 0, 0, 0, 0, 0, 0, 1)))
    }

    @Test
    fun `twenty bit prefix covers 3fff0000 and 3fff0fff but not 3fff1000`() {
        val prefix = Ipv6Prefix.parse("3fff::/20")

        assertTrue(prefix.matches(bytes(0x3FFF, 0x0000, 0x0000, 0, 0, 0, 0, 1)))
        assertTrue(prefix.matches(bytes(0x3FFF, 0x0FFF, 0x0000, 0, 0, 0, 0, 1)))
        assertFalse(prefix.matches(bytes(0x3FFF, 0x1000, 0x0000, 0, 0, 0, 0, 1)))
    }

    @Test
    fun `address of unexpected size never matches`() {
        val prefix = Ipv6Prefix.parse("fc00::/7")

        assertFalse(prefix.matches(ByteArray(4)))
        assertFalse(prefix.matches(ByteArray(0)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `length out of range is rejected`() {
        Ipv6Prefix.parse("2001::/129")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing length is rejected`() {
        Ipv6Prefix.parse("2001::")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `malformed address is rejected`() {
        Ipv6Prefix.parse("2001:::1/64")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `group with too many digits is rejected`() {
        Ipv6Prefix.parse("20011::/64")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `address with too many groups is rejected`() {
        Ipv6Prefix.parse("1:2:3:4:5:6:7:8:9/64")
    }

    // Вспомогательные функции.

    private fun bytes(vararg groups: Int): ByteArray {
        require(groups.size == 8) { "IPv6 address requires 8 groups" }
        val result = ByteArray(16)
        groups.forEachIndexed { index, group ->
            result[index * 2] = ((group shr 8) and 0xFF).toByte()
            result[index * 2 + 1] = (group and 0xFF).toByte()
        }
        return result
    }

    private fun address(vararg groups: Int): ByteArray = bytes(*groups)
}
