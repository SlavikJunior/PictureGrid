package com.example.picturegrid.core.network

import java.io.IOException
import java.net.UnknownHostException

/**
 * Нарушение политики сетевых назначений.
 *
 * Тип наследуется от [UnknownHostException], чтобы соответствовать контракту
 * [okhttp3.Dns.lookup] и при этом отличаться от обычных сетевых ошибок:
 * вызывающий код распознаёт его и не запускает автоматический повтор.
 */
class ForbiddenDestinationException(
    message: String,
) : UnknownHostException(message)
