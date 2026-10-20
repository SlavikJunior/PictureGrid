package com.example.picturegrid.core.network

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * DNS-резолвинг с проверкой фактических адресов подключения.
 *
 * Адреса, полученные от [delegate], проверяются политикой **до** установления
 * соединения, и OkHttp использует именно этот результат для подключения.
 * Поэтому между проверкой и соединением нет отдельного DNS-запроса.
 *
 * Если среди результатов есть хотя бы один запрещённый адрес, весь lookup
 * отклоняется: так адрес, отвечающий и публичным, и локальным IP, не может
 * быть выбран для подключения.
 */
class SafeDns(
    private val policy: NetworkDestinationPolicy,
    private val delegate: Dns = Dns.SYSTEM,
) : Dns {

    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = delegate.lookup(hostname)
        if (addresses.isEmpty()) return addresses
        val forbidden = addresses.filter { !policy.isAllowedAddress(it) }
        if (forbidden.isNotEmpty()) {
            throw ForbiddenDestinationException("Host resolves to a forbidden address: $hostname")
        }
        return addresses
    }
}
