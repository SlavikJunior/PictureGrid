package com.example.picturegrid.core.network

import java.net.InetAddress

/**
 * Политика допустимых сетевых назначений.
 *
 * Политика применяется на всех путях, где внешний URL превращается в сетевой
 * запрос: при разборе списка изображений, при загрузке превью через Coil и
 * при загрузке оригинала. Реализация по умолчанию —
 * [PublicNetworkDestinationPolicy].
 *
 * Интерфейс существует, чтобы тесты могли подставить разрешающую политику
 * для локального [okhttp3.mockwebserver.MockWebServer]: MockWebServer всегда
 * слушает loopback-адрес, который production-политика блокирует.
 */
interface NetworkDestinationPolicy {

    /**
     * Разрешён ли запрос по указанному URL.
     *
     * Проверяются схема, отсутствие `userinfo`, синтаксис и адрес назначения
     * (IP-литерал или имя хоста). DNS-резолвинг здесь не выполняется.
     */
    fun isAllowed(url: String): Boolean

    /**
     * Разрешено ли подключение к адресу, полученному при DNS-резолвинге.
     *
     * Вызывается непосредственно перед установлением соединения, поэтому
     * проверяется именно тот адрес, к которому приложение подключается.
     */
    fun isAllowedAddress(address: InetAddress): Boolean

    companion object {
        /** Предельное количество редиректов в одной цепочке. */
        const val MAX_REDIRECTS: Int = 5
    }
}
