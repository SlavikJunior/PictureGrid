package com.example.picturegrid.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

/**
 * Проверки классификации адресов.
 *
 * Все адреса синтетические и создаются через [InetAddress.getByAddress], поэтому
 * тесты не зависят ни от системного резолвера, ни от наличия сети.
 */
class ForbiddenAddressesTest {

    // NAT64.

    @Test
    fun `nat64 well known prefix is forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(nat64WellKnown(0x7F00, 0x0001)))
    }

    @Test
    fun `nat64 local use prefix is forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(nat64LocalUse(0x7F00, 0x0001)))
    }

    @Test
    fun `nat64 local use prefix with private address is forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(nat64LocalUse(0xC0A8, 0x0101)))
    }

    @Test
    fun `public ipv6 address is allowed`() {
        assertFalse(ForbiddenAddresses.isForbidden(ipv6(0x2606, 0x4700, 0, 0, 0, 0, 0x1111, 0x1111)))
    }

    // IPv4-mapped.

    @Test
    fun `ipv4 mapped address with loopback is forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(ipv4Compatible(0x7F00, 0x0001, mapped = true)))
    }

    @Test
    fun `ipv4 mapped address with private address is forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(ipv4Compatible(0xC0A8, 0x0101, mapped = true)))
    }

    @Test
    fun `ipv4 mapped address with public address is allowed`() {
        assertFalse(ForbiddenAddresses.isForbidden(ipv4Compatible(0x0808, 0x0808, mapped = true)))
    }

    // IPv4-compatible.

    @Test
    fun `ipv4 compatible address with private address is forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(ipv4Compatible(0xC0A8, 0x0101, mapped = false)))
    }

    @Test
    fun `ipv4 compatible address with loopback is forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(ipv4Compatible(0x7F00, 0x0001, mapped = false)))
    }

    @Test
    fun `ipv4 compatible address with public address is allowed`() {
        // Форма записи сама по себе не является признаком локального
        // назначения: решение принимает вложенный IPv4-адрес.
        assertFalse(ForbiddenAddresses.isForbidden(ipv4Compatible(0x0808, 0x0808, mapped = false)))
    }

    // Специальные диапазоны IPv6.

    @Test
    fun `ietf protocol assignment ranges are forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0x2001, 0x0000, 0, 0, 0, 0, 0, 0x0001))) // Teredo
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0x2001, 0x0002, 0, 0, 0, 0, 0, 0x0001))) // benchmarking
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0x2001, 0x0020, 0, 0, 0, 0, 0, 0x0001))) // ORCHIDv2
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0x2001, 0x0030, 0, 0, 0, 0, 0, 0x0001))) // Drone
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0x2001, 0x0DB8, 0, 0, 0, 0, 0, 0x0001))) // documentation
    }

    @Test
    fun `public addresses in 2001 space are allowed`() {
        // Второе 16-битное слово больше 0x01ff, поэтому блок 2001::/23 их не
        // затрагивает.
        assertFalse(ForbiddenAddresses.isForbidden(ipv6(0x2001, 0x4860, 0x4860, 0, 0, 0, 0, 0x8888)))
        assertFalse(ForbiddenAddresses.isForbidden(ipv6(0x2001, 0x1900, 0, 0, 0, 0, 0, 0x0001)))
    }

    @Test
    fun `6to4 addresses are forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0x2002, 0x7F00, 0x0001, 0, 0, 0, 0, 0)))
    }

    @Test
    fun `documentation and sid ranges are forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0x3FFF, 0x0000, 0, 0, 0, 0, 0, 0x0001))) // 3fff::/20
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0x5F00, 0x0000, 0, 0, 0, 0, 0, 0x0001))) // 5f00::/16
    }

    @Test
    fun `addresses outside documentation and sid ranges are allowed`() {
        assertFalse(ForbiddenAddresses.isForbidden(ipv6(0x3FFF, 0x1000, 0, 0, 0, 0, 0, 0x0001)))
        assertFalse(ForbiddenAddresses.isForbidden(ipv6(0x5F01, 0x0000, 0, 0, 0, 0, 0, 0x0001)))
    }

    @Test
    fun `local and multicast ranges are forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0xFC00, 0x0000, 0, 0, 0, 0, 0, 0x0001))) // ULA
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0xFD12, 0x3456, 0x789A, 0, 0, 0, 0, 0x0001)))
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0xFE80, 0x0000, 0, 0, 0, 0, 0, 0x0001))) // link-local
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0xFEC0, 0x0000, 0, 0, 0, 0, 0, 0x0001))) // site-local
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0xFF02, 0x0000, 0, 0, 0, 0, 0, 0x0001))) // multicast
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0x0100, 0x0000, 0, 0, 0, 0, 0, 0x0001))) // discard-only
    }

    @Test
    fun `unspecified and loopback are forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0, 0, 0, 0, 0, 0, 0, 0)))
        assertTrue(ForbiddenAddresses.isForbidden(ipv6(0, 0, 0, 0, 0, 0, 0, 0x0001)))
    }

    // IPv4.

    @Test
    fun `non public ipv4 addresses are forbidden`() {
        assertTrue(ForbiddenAddresses.isForbidden(ipv4(127, 0, 0, 1)))
        assertTrue(ForbiddenAddresses.isForbidden(ipv4(10, 0, 0, 1)))
        assertTrue(ForbiddenAddresses.isForbidden(ipv4(172, 16, 0, 1)))
        assertTrue(ForbiddenAddresses.isForbidden(ipv4(192, 168, 1, 1)))
        assertTrue(ForbiddenAddresses.isForbidden(ipv4(169, 254, 169, 254)))
        assertTrue(ForbiddenAddresses.isForbidden(ipv4(100, 64, 0, 1)))
        assertTrue(ForbiddenAddresses.isForbidden(ipv4(0, 0, 0, 0)))
        assertTrue(ForbiddenAddresses.isForbidden(ipv4(224, 0, 0, 1)))
        assertTrue(ForbiddenAddresses.isForbidden(ipv4(255, 255, 255, 255)))
    }

    @Test
    fun `public ipv4 address is allowed`() {
        assertFalse(ForbiddenAddresses.isForbidden(ipv4(8, 8, 8, 8)))
        assertFalse(ForbiddenAddresses.isForbidden(ipv4(93, 184, 216, 34)))
    }

    // Вспомогательные функции.

    private fun ipv4(a: Int, b: Int, c: Int, d: Int): InetAddress =
        InetAddress.getByAddress(byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte()))

    /** IPv6-адрес из восьми 16-битных групп. */
    private fun ipv6(vararg groups: Int): InetAddress {
        require(groups.size == 8) { "IPv6 address requires 8 groups" }
        val bytes = ByteArray(16)
        groups.forEachIndexed { index, group ->
            bytes[index * 2] = ((group shr 8) and 0xFF).toByte()
            bytes[index * 2 + 1] = (group and 0xFF).toByte()
        }
        return InetAddress.getByAddress(bytes)
    }

    /** `64:ff9b::<hi>:<lo>` — well-known префикс NAT64 (RFC 6052). */
    private fun nat64WellKnown(hi: Int, lo: Int): InetAddress =
        ipv6(0x0064, 0xFF9B, 0x0000, 0x0000, 0x0000, 0x0000, hi, lo)

    /** `64:ff9b:1::<hi>:<lo>` — local-use префикс NAT64 (RFC 8215). */
    private fun nat64LocalUse(hi: Int, lo: Int): InetAddress =
        ipv6(0x0064, 0xFF9B, 0x0001, 0x0000, 0x0000, 0x0000, hi, lo)

    /**
     * IPv4-адрес, встроенный в IPv6: `::ffff:<hi>:<lo>` (mapped) либо
     * `::<hi>:<lo>` (compatible).
     */
    private fun ipv4Compatible(hi: Int, lo: Int, mapped: Boolean): InetAddress =
        if (mapped) {
            ipv6(0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0xFFFF, hi, lo)
        } else {
            ipv6(0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, hi, lo)
        }
}
