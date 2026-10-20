package com.example.picturegrid.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetAddress

/**
 * Строгая политика сетевых назначений.
 *
 * Разрешены только публичные HTTP(S)-адреса:
 * - схема `http` или `https` (обе нужны существующему контракту MVP);
 * - без `userinfo` (`user:password@host`);
 * - хост — либо публичный IP-адрес, либо доменное имя с точкой,
 *   не относящееся к локальным зонам.
 *
 * Произвольные публичные домены **не** ограничиваются списком: приложение
 * получает ссылки на изображения с разных хостов, поэтому фиксированный
 * allowlist здесь ломал бы функциональность.
 */
object PublicNetworkDestinationPolicy : NetworkDestinationPolicy {

    /** Локальные зоны, которые не могут указывать на публичный сервис. */
    private val LOCAL_HOST_SUFFIXES = listOf(
        "localhost",
        "local",
        "internal",
        "home.arpa",
    )

    private val IPV4_LITERAL = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
    private val NUMERIC_HOST = Regex("^\\d+(\\.\\d+){0,3}$")
    private val IPV6_CHARS = Regex("^[0-9a-fA-F:.]+$")

    override fun isAllowed(url: String): Boolean {
        val parsed = url.toHttpUrlOrNull() ?: return false
        return isAllowed(parsed)
    }

    /** Проверяет URL, разобранный [okhttp3.HttpUrl]. */
    fun isAllowed(url: HttpUrl): Boolean {
        if (url.scheme != "http" && url.scheme != "https") return false
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) return false
        val host = url.host.lowercase()
        if (host.isEmpty()) return false

        val literal = parseIpLiteral(host)
        if (literal != null) return !ForbiddenAddresses.isForbidden(literal)
        return isAllowedHostName(host)
    }

    override fun isAllowedAddress(address: InetAddress): Boolean =
        !ForbiddenAddresses.isForbidden(address)

    /**
     * Разрешено ли имя хоста.
     *
     * Отклоняются однокомпонентные имена (обычно разрешаются через локальный
     * поисковый домен) и имена в локальных зонах. Сравнение выполняется по
     * границе домена: `host == zone` или `host.endsWith(".$zone")`.
     */
    private fun isAllowedHostName(host: String): Boolean {
        val normalized = host.removeSuffix(".")
        if (normalized.isEmpty()) return false
        if ('.' !in normalized) return false
        return LOCAL_HOST_SUFFIXES.none { zone ->
            normalized == zone || normalized.endsWith(".$zone")
        }
    }

    /**
     * Возвращает IP-адрес, если [host] — IP-литерал, и `null` иначе.
     *
     * DNS-резолвинг не выполняется: разбираются только числовые формы IPv4
     * (включая сокращённые `127.1` и целочисленные `2130706433`) и IPv6.
     * Экзотические формы (`0x7f.0.0.1`) не распознаются здесь, но
     * отсекаются проверкой адреса на этапе подключения.
     */
    private fun parseIpLiteral(host: String): InetAddress? = when {
        IPV4_LITERAL.matches(host) -> literalAddress(host)
        NUMERIC_HOST.matches(host) -> literalAddress(host)
        ':' in host && IPV6_CHARS.matches(host) && host.count { it == ':' } >= 2 -> literalAddress(host)
        else -> null
    }

    private fun literalAddress(host: String): InetAddress? =
        runCatching { InetAddress.getByName(host) }.getOrNull()
}
