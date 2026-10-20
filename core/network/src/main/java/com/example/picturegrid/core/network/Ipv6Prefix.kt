package com.example.picturegrid.core.network

/**
 * Префикс IPv6-адреса: последовательность байт и длина в битах.
 *
 * Префикс задаётся текстом (`"64:ff9b:1::/48"`), поэтому длина в битах и
 * последовательность байт всегда согласованы: расхождение между ними
 * конструктивно невозможно. Это исключает ошибку вида «префикс короче, чем
 * заявленная длина», из-за которой проверка префикса всегда возвращала бы
 * `false` и молча пропускала бы запрещённые адреса.
 *
 * Разбор выполняется без обращения к системному резолверу: класс не должен
 * зависеть от DNS и от платформенной трактовки адресов.
 */
internal class Ipv6Prefix private constructor(
    private val bytes: ByteArray,
    val lengthBits: Int,
    private val text: String,
) {

    init {
        require(lengthBits in 1..128) {
            "Prefix length must be in 1..128, got $lengthBits ($text)"
        }
        require(bytes.size == BYTE_COUNT) {
            "Prefix must contain $BYTE_COUNT bytes, got ${bytes.size} ($text)"
        }
    }

    /**
     * Совпадает ли адрес с префиксом.
     *
     * @param address 16 байт адреса в сетевом порядке.
     */
    fun matches(address: ByteArray): Boolean {
        if (address.size != BYTE_COUNT) return false
        var remaining = lengthBits
        var index = 0
        while (remaining > 0) {
            val significant = minOf(8, remaining)
            val mask = (0xFF shl (8 - significant)) and 0xFF
            val expected = bytes[index].toInt() and 0xFF
            val actual = address[index].toInt() and 0xFF
            if ((actual and mask) != (expected and mask)) return false
            remaining -= significant
            index++
        }
        return true
    }

    override fun toString(): String = text

    companion object {

        /** Размер IPv6-адреса в байтах. */
        const val BYTE_COUNT = 16

        /**
         * Разбирает префикс в текстовой форме: `"2001::/23"`.
         *
         * @throws IllegalArgumentException если форма некорректна.
         */
        fun parse(text: String): Ipv6Prefix {
            val separator = text.indexOf('/')
            require(separator > 0) { "Prefix must contain a length: $text" }
            val address = text.substring(0, separator)
            val length = text.substring(separator + 1).toIntOrNull()
                ?: throw IllegalArgumentException("Invalid prefix length: $text")
            require(length in 1..128) { "Prefix length must be in 1..128: $text" }
            return Ipv6Prefix(parseAddress(address), length, text)
        }

        /**
         * Разбирает IPv6-адрес в 16 байт.
         *
         * Поддерживаются hex-группы и сжатие `::`. Обращение к DNS не
         * выполняется.
         */
        private fun parseAddress(text: String): ByteArray {
            val compressed = text.indexOf("::")
            require(compressed < 0 || text.indexOf("::", compressed + 1) < 0) {
                "Only one '::' is allowed: $text"
            }
            val head: List<String>
            val tail: List<String>
            if (compressed >= 0) {
                head = splitGroups(text.substring(0, compressed))
                tail = splitGroups(text.substring(compressed + 2))
            } else {
                head = splitGroups(text)
                tail = emptyList()
            }
            val explicit = head.size + tail.size
            require(explicit <= BYTE_COUNT / 2) { "Too many groups in IPv6 address: $text" }
            if (compressed >= 0) {
                require(explicit < BYTE_COUNT / 2) { "'::' must replace at least one group: $text" }
            } else {
                require(explicit == BYTE_COUNT / 2) { "IPv6 address must contain 8 groups: $text" }
            }

            val result = ByteArray(BYTE_COUNT)
            var offset = 0
            for (group in head) {
                writeGroup(group, result, offset)
                offset += 2
            }
            var tailOffset = BYTE_COUNT - tail.size * 2
            for (group in tail) {
                writeGroup(group, result, tailOffset)
                tailOffset += 2
            }
            return result
        }

        private fun splitGroups(text: String): List<String> =
            if (text.isEmpty()) emptyList() else text.split(':').filter { it.isNotEmpty() }

        private fun writeGroup(group: String, target: ByteArray, offset: Int) {
            require(group.length in 1..4) { "Invalid IPv6 group: $group" }
            val value = group.toIntOrNull(16)
                ?: throw IllegalArgumentException("Invalid IPv6 group: $group")
            target[offset] = ((value shr 8) and 0xFF).toByte()
            target[offset + 1] = (value and 0xFF).toByte()
        }
    }
}
