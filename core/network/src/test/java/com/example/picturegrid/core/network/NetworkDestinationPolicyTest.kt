package com.example.picturegrid.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.InetAddress

/**
 * Проверки политики допустимых сетевых назначений.
 *
 * Тесты не зависят от интернета и от доступности реальных доменов:
 * проверяется только синтаксическая часть политики.
 */
class NetworkDestinationPolicyTest {

    private val policy = PublicNetworkDestinationPolicy

    // Разрешённые адреса.

    @Test
    fun `public https url is allowed`() {
        assertTrue(policy.isAllowed("https://example.com/images/photo.png"))
    }

    @Test
    fun `public http url is allowed`() {
        assertTrue(policy.isAllowed("http://example.com/images/photo.png"))
    }

    @Test
    fun `public url with port and query is allowed`() {
        assertTrue(policy.isAllowed("https://example.com:8443/photo.png?w=100&h=100"))
    }

    @Test
    fun `internationalized domain is allowed in punycode form`() {
        assertTrue(policy.isAllowed("https://xn--e1afmkfd.xn--p1ai/photo.png"))
    }

    // Схема и синтаксис.

    @Test
    fun `url without scheme is rejected`() {
        assertFalse(policy.isAllowed("example.com/photo.png"))
        assertFalse(policy.isAllowed("//example.com/photo.png"))
    }

    @Test
    fun `unsupported schemes are rejected`() {
        assertFalse(policy.isAllowed("file:///etc/passwd"))
        assertFalse(policy.isAllowed("content://media/external/images/1"))
        assertFalse(policy.isAllowed("javascript:alert(1)"))
        assertFalse(policy.isAllowed("ftp://example.com/photo.png"))
        assertFalse(policy.isAllowed("data:image/png;base64,iVBORw0KGgo="))
    }

    @Test
    fun `url without host is rejected`() {
        assertFalse(policy.isAllowed("http://"))
        assertFalse(policy.isAllowed("http://:8080/photo.png"))
        assertFalse(policy.isAllowed("https://?query=1"))
    }

    @Test
    fun `malformed url is rejected`() {
        assertFalse(policy.isAllowed("not a url at all"))
        assertFalse(policy.isAllowed("http://exa mple.com/photo.png"))
    }

    @Test
    fun `url with userinfo is rejected`() {
        assertFalse(policy.isAllowed("http://user:password@example.com/photo.png"))
        assertFalse(policy.isAllowed("http://user@example.com/photo.png"))
        assertFalse(policy.isAllowed("https://user:password@example.com/photo.png"))
    }

    // Loopback.

    @Test
    fun `loopback ipv4 is rejected`() {
        assertFalse(policy.isAllowed("http://127.0.0.1/photo.png"))
        assertFalse(policy.isAllowed("http://127.1.2.3/photo.png"))
        assertFalse(policy.isAllowed("http://127.0.0.1:8080/photo.png"))
    }

    @Test
    fun `loopback ipv6 is rejected`() {
        assertFalse(policy.isAllowed("http://[::1]/photo.png"))
        assertFalse(policy.isAllowed("http://[::1]:8080/photo.png"))
        assertFalse(policy.isAllowed("http://[0:0:0:0:0:0:0:1]/photo.png"))
    }

    // Private и link-local.

    @Test
    fun `private ipv4 ranges are rejected`() {
        assertFalse(policy.isAllowed("http://10.0.0.5/photo.png"))
        assertFalse(policy.isAllowed("http://172.16.0.1/photo.png"))
        assertFalse(policy.isAllowed("http://172.31.255.254/photo.png"))
        assertFalse(policy.isAllowed("http://192.168.1.1/photo.png"))
        assertFalse(policy.isAllowed("http://100.64.0.1/photo.png"))
    }

    @Test
    fun `link local addresses are rejected`() {
        assertFalse(policy.isAllowed("http://169.254.169.254/latest/meta-data/"))
        assertFalse(policy.isAllowed("http://169.254.1.1/photo.png"))
        assertFalse(policy.isAllowed("http://[fe80::1]/photo.png"))
        assertFalse(policy.isAllowed("http://[fe80::a00:27ff:fe4e:66a1]/photo.png"))
    }

    @Test
    fun `unspecified and special addresses are rejected`() {
        assertFalse(policy.isAllowed("http://0.0.0.0/photo.png"))
        assertFalse(policy.isAllowed("http://0.1.2.3/photo.png"))
        assertFalse(policy.isAllowed("http://[::]/photo.png"))
        assertFalse(policy.isAllowed("http://224.0.0.1/photo.png"))
        assertFalse(policy.isAllowed("http://239.255.255.255/photo.png"))
        assertFalse(policy.isAllowed("http://255.255.255.255/photo.png"))
        assertFalse(policy.isAllowed("http://240.0.0.1/photo.png"))
        assertFalse(policy.isAllowed("http://[ff02::1]/photo.png"))
        assertFalse(policy.isAllowed("http://[fc00::1]/photo.png"))
        assertFalse(policy.isAllowed("http://[fd12:3456:789a::1]/photo.png"))
    }

    @Test
    fun `ipv4 mapped ipv6 address is rejected`() {
        assertFalse(policy.isAllowed("http://[::ffff:127.0.0.1]/photo.png"))
        assertFalse(policy.isAllowed("http://[::ffff:10.0.0.1]/photo.png"))
    }

    // NAT64 и переходные механизмы.

    @Test
    fun `nat64 prefixes are rejected`() {
        // Well-known префикс (RFC 6052) и local-use префикс (RFC 8215).
        assertFalse(policy.isAllowed("http://[64:ff9b::7f00:1]/photo.png"))
        assertFalse(policy.isAllowed("http://[64:ff9b:1::7f00:1]/photo.png"))
        assertFalse(policy.isAllowed("http://[64:ff9b:1::c0a8:101]/photo.png"))
    }

    @Test
    fun `ipv4 compatible ipv6 address is decided by the embedded address`() {
        // Форма записи сама по себе не является признаком локального
        // назначения: решение принимает вложенный IPv4-адрес.
        assertFalse(policy.isAllowed("http://[::192.168.1.1]/photo.png"))
        assertFalse(policy.isAllowed("http://[::127.0.0.1]/photo.png"))
        assertTrue(policy.isAllowed("http://[::8.8.8.8]/photo.png"))
    }

    @Test
    fun `special ipv6 ranges are rejected and public ones are allowed`() {
        assertFalse(policy.isAllowed("http://[2001::1]/photo.png")) // Teredo
        assertFalse(policy.isAllowed("http://[2001:db8::1]/photo.png")) // документация
        assertFalse(policy.isAllowed("http://[2002:7f00:1::1]/photo.png")) // 6to4
        assertFalse(policy.isAllowed("http://[fec0::1]/photo.png")) // site-local
        assertFalse(policy.isAllowed("http://[3fff::1]/photo.png")) // документация
        assertFalse(policy.isAllowed("http://[5f00::1]/photo.png")) // SRv6 SIDs
        assertTrue(policy.isAllowed("http://[2001:4860:4860::8888]/photo.png"))
        assertTrue(policy.isAllowed("http://[2606:4700::1111]/photo.png"))
    }

    // Локальные имена.

    @Test
    fun `local host names are rejected`() {
        assertFalse(policy.isAllowed("http://localhost/photo.png"))
        assertFalse(policy.isAllowed("http://localhost./photo.png"))
        assertFalse(policy.isAllowed("http://LOCALHOST/photo.png"))
        assertFalse(policy.isAllowed("http://router/photo.png"))
        assertFalse(policy.isAllowed("http://nas/photo.png"))
        assertFalse(policy.isAllowed("http://printer.local/photo.png"))
        assertFalse(policy.isAllowed("http://service.internal/photo.png"))
        assertFalse(policy.isAllowed("http://host.home.arpa/photo.png"))
    }

    @Test
    fun `domain boundary check does not match similar public names`() {
        // Граница домена проверяется корректно: похожие имена остаются разрешёнными.
        assertTrue(policy.isAllowed("https://notlocalhost.example.com/photo.png"))
        assertTrue(policy.isAllowed("https://mylocalhost.example.com/photo.png"))
        assertTrue(policy.isAllowed("https://example.com/photo.png"))
        assertTrue(policy.isAllowed("https://localhost.example.com/photo.png"))
    }

    // Нормализация.

    @Test
    fun `host case and trailing dot are normalized`() {
        assertTrue(policy.isAllowed("https://EXAMPLE.COM/photo.png"))
        assertTrue(policy.isAllowed("https://example.com./photo.png"))
        assertFalse(policy.isAllowed("http://LOCALHOST./photo.png"))
    }

    @Test
    fun `ipv6 literal with brackets and port is handled`() {
        assertTrue(policy.isAllowed("https://[2001:4860:4860::8888]/photo.png"))
        assertFalse(policy.isAllowed("https://[2001:4860:4860::8888]:8443/photo.png").not())
    }

    @Test
    fun `loopback addresses are rejected regardless of the notation`() {
        // Классификатор проверяется на синтетических адресах: результат не
        // должен зависеть от системного резолвера и от наличия сети.
        assertFalse(policy.isAllowedAddress(InetAddress.getByAddress(byteArrayOf(127.toByte(), 0, 0, 1))))
        assertFalse(policy.isAllowedAddress(InetAddress.getByAddress(byteArrayOf(10, 0, 0, 1))))
        assertFalse(
            policy.isAllowedAddress(InetAddress.getByAddress(byteArrayOf(169.toByte(), 254.toByte(), 0, 1))),
        )
    }

    @Test
    fun `short and integer ip notations are rejected when the platform resolves them to loopback`() {
        // Платформы по-разному трактуют сокращённые и целочисленные формы
        // (inet_aton против inet_pton), поэтому проверяется инвариант: если
        // система распознала форму как loopback, политика обязана её отклонить.
        for (form in listOf("127.1", "2130706433")) {
            val resolved = runCatching { InetAddress.getByName(form) }.getOrNull()
            assumeTrue(
                "платформа не распознаёт форму $form как loopback",
                resolved != null && resolved.isLoopbackAddress,
            )
            val address = requireNotNull(resolved)
            assertFalse("форма $form не должна быть разрешена", policy.isAllowedAddress(address))
            assertFalse("форма $form не должна быть разрешена", policy.isAllowed("http://$form/photo.png"))
        }
    }
}
