package com.example.picturegrid.core.network

import java.net.InetAddress

/**
 * Проверка принадлежности адреса к специальным (непубличным) диапазонам.
 *
 * Диапазоны перечислены явно, чтобы правило проверки было аудируемым.
 *
 * IPv6-адреса, в которые встроен IPv4-адрес, оцениваются **по вложенному
 * IPv4**: форма записи сама по себе не является признаком локального
 * назначения. Переходные механизмы (NAT64, 6to4) запрещены целиком: они не
 * являются назначением для обычных публичных сервисов, а вложенный IPv4 в них
 * размещается по разным смещениям в зависимости от длины префикса.
 */
internal object ForbiddenAddresses {

    /**
     * Диапазоны IPv4, в которые подключаться запрещено.
     *
     * - `0.0.0.0/8` — «this network» и unspecified;
     * - `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16` — private;
     * - `100.64.0.0/10` — CGNAT (RFC 6598);
     * - `127.0.0.0/8` — loopback;
     * - `169.254.0.0/16` — link-local, включая `169.254.169.254`;
     * - `192.0.0.0/24`, `192.88.99.0/24` — служебные диапазоны IANA;
     * - `192.0.2.0/24`, `198.51.100.0/24`, `203.0.113.0/24` — документационные;
     * - `198.18.0.0/15` — benchmarking;
     * - `224.0.0.0/4` — multicast;
     * - `240.0.0.0/4` — reserved и broadcast.
     *
     * Список покрывает все непубличные диапазоны реестра IANA. Глобально
     * маршрутизируемые специальные диапазоны (`192.31.196.0/24`,
     * `192.52.193.0/24`, `192.175.48.0/24`) намеренно не заблокированы.
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

    /**
     * Запрещённые диапазоны IPv6.
     *
     * - `::/128` — unspecified, `::1/128` — loopback;
     * - `64:ff9b::/96` — NAT64, well-known prefix (RFC 6052);
     * - `64:ff9b:1::/48` — NAT64, local-use prefix (RFC 8215);
     * - `100::/64` — discard-only (RFC 6666);
     * - `2001::/23` — назначения IETF (RFC 2928): Teredo `2001::/32`,
     *   benchmarking `2001:2::/48`, ORCHIDv2 `2001:20::/28`, Drone Remote ID
     *   `2001:30::/28`, AMT, AS112;
     * - `2001:db8::/32` — документация (RFC 3849); этот диапазон лежит
     *   **вне** `2001::/23` (второе 16-битное слово `0x0db8` больше
     *   `0x01ff`), поэтому указывается отдельной записью;
     * - `2002::/16` — 6to4 (RFC 3056);
     * - `3fff::/20` — документация (RFC 9637);
     * - `5f00::/16` — SRv6 SIDs (RFC 9602);
     * - `fc00::/7` — unique local (RFC 4193);
     * - `fe80::/10` — link-local;
     * - `fec0::/10` — site-local, устаревший (RFC 3879);
     * - `ff00::/8` — multicast.
     *
     * Диапазон `2001::/23` не затрагивает публичные адреса Google
     * (`2001:4860::/32`, `2001:1900::/28`) и других операторов: второе
     * 16-битное слово в них больше `0x01ff`.
     *
     * Глобально маршрутизируемые диапазоны, не предназначенные для обычных
     * запросов (например `2620:4f:8000::/48` — AS112), в список не входят.
     */
    private val IPV6_PREFIXES: List<Ipv6Prefix> = listOf(
        Ipv6Prefix.parse("::/128"), // unspecified
        Ipv6Prefix.parse("::1/128"), // loopback
        Ipv6Prefix.parse("64:ff9b::/96"), // NAT64 well-known prefix
        Ipv6Prefix.parse("64:ff9b:1::/48"), // NAT64 local-use prefix
        Ipv6Prefix.parse("100::/64"), // discard-only
        Ipv6Prefix.parse("2001::/23"), // IETF protocol assignments
        Ipv6Prefix.parse("2001:db8::/32"), // documentation (вне 2001::/23)
        Ipv6Prefix.parse("2002::/16"), // 6to4
        Ipv6Prefix.parse("3fff::/20"), // documentation
        Ipv6Prefix.parse("5f00::/16"), // SRv6 SIDs
        Ipv6Prefix.parse("fc00::/7"), // unique local
        Ipv6Prefix.parse("fe80::/10"), // link-local
        Ipv6Prefix.parse("fec0::/10"), // site-local (deprecated)
        Ipv6Prefix.parse("ff00::/8"), // multicast
    )

    /**
     * Префиксы IPv6, в которые встроен IPv4-адрес.
     *
     * Для таких адресов решение принимает **вложенный IPv4-адрес** (последние
     * четыре байта) по тем же правилам, что и для обычного IPv4-адреса:
     * - `::ffff:0:0/96` — IPv4-mapped (RFC 4291);
     * - `::/96` — IPv4-compatible, устаревшая форма (RFC 4291).
     */
    private val IPV4_EMBEDDED_PREFIXES: List<Ipv6Prefix> = listOf(
        Ipv6Prefix.parse("::ffff:0:0/96"), // IPv4-mapped
        Ipv6Prefix.parse("::/96"), // IPv4-compatible (deprecated)
    )

    /** Смещение вложенного IPv4-адреса в IPv6-адресе. */
    private const val IPV4_EMBEDDED_OFFSET = 12

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
        // Вложенный IPv4 решает: форма записи сама по себе не признак
        // локального назначения, поэтому `::8.8.8.8` допустим, а
        // `::192.168.1.1` — нет.
        for (prefix in IPV4_EMBEDDED_PREFIXES) {
            if (prefix.matches(bytes)) {
                val embedded = bytes.copyOfRange(IPV4_EMBEDDED_OFFSET, IPV4_EMBEDDED_OFFSET + 4)
                return isForbiddenIpv4(embedded.toIpv4Long())
            }
        }
        return IPV6_PREFIXES.any { it.matches(bytes) }
    }

    private fun ByteArray.toIpv4Long(): Long =
        ((this[0].toInt() and 0xFF).toLong() shl 24) or
            ((this[1].toInt() and 0xFF).toLong() shl 16) or
            ((this[2].toInt() and 0xFF).toLong() shl 8) or
            (this[3].toInt() and 0xFF).toLong()
}
