package com.example.picturegrid.core.network

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Проверки DNS-фильтра: адреса, к которым приложение подключается,
 * проверяются непосредственно перед установлением соединения.
 */
class SafeDnsTest {

    private val loopback = InetAddress.getByName("127.0.0.1")
    private val privateAddress = InetAddress.getByName("192.168.1.10")
    private val publicAddress = InetAddress.getByName("93.184.216.34")

    @Test
    fun `public address is allowed`() {
        val dns = SafeDns(PublicNetworkDestinationPolicy, FakeDns(listOf(publicAddress)))
        assertEquals(listOf(publicAddress), dns.lookup("images.example.com"))
    }

    @Test
    fun `loopback address is rejected`() {
        assertRejected(listOf(loopback))
    }

    @Test
    fun `private address is rejected`() {
        assertRejected(listOf(privateAddress))
    }

    @Test
    fun `mixed answer is rejected entirely`() {
        // Если среди ответов есть локальный адрес, не подключаемся ни к одному:
        // выбор другого адреса позволял бы обойти проверку.
        assertRejected(listOf(publicAddress, loopback))
    }

    @Test
    fun `empty answer is passed through`() {
        val dns = SafeDns(PublicNetworkDestinationPolicy, FakeDns(emptyList()))
        assertTrue(dns.lookup("images.example.com").isEmpty())
    }

    @Test
    fun `dns failure is propagated`() {
        val dns = SafeDns(PublicNetworkDestinationPolicy, FakeDns(addresses = emptyList(), failure = UnknownHostException("no dns")))
        try {
            dns.lookup("images.example.com")
            fail("Ожидалось исключение")
        } catch (e: UnknownHostException) {
            assertEquals("no dns", e.message)
        }
    }

    private fun assertRejected(addresses: List<InetAddress>) {
        val dns = SafeDns(PublicNetworkDestinationPolicy, FakeDns(addresses))
        try {
            dns.lookup("images.example.com")
            fail("Ожидался отказ DNS-фильтра")
        } catch (e: ForbiddenDestinationException) {
            assertTrue(e.message?.contains("forbidden") == true)
        }
    }

    private class FakeDns(
        private val addresses: List<InetAddress>,
        private val failure: UnknownHostException? = null,
    ) : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            failure?.let { throw it }
            return addresses
        }
    }
}
