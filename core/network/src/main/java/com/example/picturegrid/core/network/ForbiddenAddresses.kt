package com.example.picturegrid.core.network

import java.net.InetAddress

/**
 * Проверка принадлежности адреса к специальным (непубличным) диапазонам.
 *
 * Диапазоны перечислены явно, чтобы правило проверки было аудируемым.
 * IPv4-адреса embedded в IPv6 (IPv4-mapped, NAT64, 6to4) проверяются
 * по вложенному адресу.
 */
internal object ForbiddenAddresses {

    /**
     * Диапазоны IPv4, в которые подключаться запрещено.
     *
     * - `0.0.0.0/8` — «this network» и unspecified;
     * - `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16` — private;
     * - `100.64.0.0/10` — CGNAT;
     * - `127.0.0.0/8` — loopback;
     * - `169.254.0.0/16` — link-local, включая `169.254.169.254`;
     * - `192.0.0.0/24`, `192.88.99.0/24` — служебные диапазоны IANA;
     * - `192.0.2.0/24`, `198.51.100.0/24`, `203.0.113.0/24` — документационные;
     * - `198.18.0.0/15` — benchmarking;
     * - `224.0.0.0/4` — multicast;
     * - `240.0.0.0/4` — reserved и broadcast.
     */
    private val IPV4_RANGES: List<LongRange> = listOf(
        0x00000000L..0x00FFFFFFL, // 0.0.0.0/8
        0x0A000000L..0x0AFFFFFFL, // 10.0.0.0/8
        0x64400000L..0x647FFFFFL, // 100.64.0.0/10
        0x7F000000L..0x7FFFFFFFL, // 127.0.0.0/8
        0xA9FE0000L..0xA9FEFFFFL, // 169.254.0.0/16
        0xAC100000L..0xAC1FFFFFL, // 172.16.0.0/12
        0xC0000000L..0xC00000FFL, // 192.0.0.0/24
        0xC0000200L..0xC00002FFL, // 192.0.2.0/24
        0xC0586300L..0xC05863FFL, // 192.88.99.0/24
        0xC0A80000L..0xC0A8FFFFL, // 192.168.0.0/16
        0xC6120000L..0xC613FFFFL, // 198.18.0.0/15
        0xC6336400L..0xC63364FFL, // 198.51.100.0/24
        0xCB007100L..0xCB0071FFL, // 203.0.113.0/24
        0xE0000000L..0xEFFFFFFFL, // 224.0.0.0/4
        0xF0000000L..0xFFFFFFFFL, // 240.0.0.0/4
    )

    /** Запрещённые адреса IPv6 в виде префиксов (байты и число значимых бит). */
    private val IPV6_PREFIXES: List<Pair<IntArray, Int>> = listOf(
        intArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00) to 128, // ::
        intArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01) to 128, // ::1
        intArrayOf(0x00, 0x64, 0xFF, 0x9B, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00) to 96, // 64:ff9b::/96 (NAT64)
        intArrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00) to 64, // 100::/64 (discard-only)
        intArrayOf(0x20, 0x01, 0x0D, 0xB8) to 32, // 2001:db8::/32 (documentation)
        intArrayOf(0x20, 0x02) to 16, // 2002::/16 (6to4)
        intArrayOf(0xFC) to 7, // fc00::/7 (unique local)
        intArrayOf(0xFE, 0x80) to 10, // fe80::/10 (link-local)
        intArrayOf(0xFF) to 8, // ff00::/8 (multicast)
    )

    /** IPv4-mapped адреса `::ffff:0:0/96`. */
    private val IPV4_MAPPED_PREFIX = intArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF) to 96

    fun isForbidden(address: InetAddress): Boolean {
        val bytes = address.address
        return when (bytes.size) {
            4 -> isForbiddenIpv4(bytes.toIpv4Long())
            16 -> isForbiddenIpv6(bytes)
            else -> true
        }
    }

    private fun isForbiddenIpv4(value: Long): Boolean = IPV4_RANGES.any { value in it }

    private fun isForbiddenIpv6(bytes: ByteArray): Boolean {
        // IPv4-mapped: вложенный адрес проверяется как обычный IPv4.
        if (bytes.hasPrefix(IPV4_MAPPED_PREFIX.first, IPV4_MAPPED_PREFIX.second)) {
            return isForbiddenIpv4(bytes.copyOfRange(12, 16).toIpv4Long())
        }
        // NAT64 и 6to4: вложенный IPv4 тоже запрещён.
        if (bytes.hasPrefix(intArrayOf(0x00, 0x64, 0xFF, 0x9B), 96)) {
            return isForbiddenIpv4(bytes.copyOfRange(12, 16).toIpv4Long())
        }
        if (bytes.hasPrefix(intArrayOf(0x20, 0x02), 16)) {
            return isForbiddenIpv4(bytes.copyOfRange(2, 6).toIpv4Long())
        }
        return IPV6_PREFIXES.any { (prefix, bits) -> bytes.hasPrefix(prefix, bits) }
    }

    private fun ByteArray.toIpv4Long(): Long =
        ((this[0].toInt() and 0xFF).toLong() shl 24) or
            ((this[1].toInt() and 0xFF).toLong() shl 16) or
            ((this[2].toInt() and 0xFF).toLong() shl 8) or
            (this[3].toInt() and 0xFF).toLong()

    /** Проверяет совпадение префикса с учётом числа значимых бит. */
    private fun ByteArray.hasPrefix(prefix: IntArray, bits: Int): Boolean {
        var remaining = bits
        var index = 0
        while (remaining > 0) {
            if (index >= size || index >= prefix.size) return false
            val significant = minOf(8, remaining)
            val expected = prefix[index].toInt() and 0xFF
            val actual = this[index].toInt() and 0xFF
            if (significant == 8) {
                if (actual != expected) return false
            } else {
                val mask = (0xFF shl (8 - significant)) and 0xFF
                if ((actual and mask) != (expected and mask)) return false
            }
            remaining -= significant
            index++
        }
        return true
    }
}
