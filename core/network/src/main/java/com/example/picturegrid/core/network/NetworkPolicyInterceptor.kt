package com.example.picturegrid.core.network

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Проверка адреса назначения и контролируемая обработка редиректов.
 *
 * Автоматические редиректы OkHttp отключаются (см.
 * [okhttp3.OkHttpClient.Builder.followRedirects]), а переходы обрабатываются
 * здесь: адрес каждой цели проверяется **до** отправки запроса, поэтому
 * запрос к запрещённому адресу не выполняется вовсе.
 *
 * Ограничения:
 * - не более [maxRedirects] переходов в одной цепочке;
 * - `Location` разрешается относительно текущего URL (в том числе относительный);
 * - тела промежуточных ответов закрываются;
 * - `Authorization` и `Cookie` не переносятся при смене origin;
 * - понижение `https` → `http` отклоняется.
 */
class NetworkPolicyInterceptor(
    private val policy: NetworkDestinationPolicy,
    private val maxRedirects: Int = NetworkDestinationPolicy.MAX_REDIRECTS,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        var redirects = 0
        while (true) {
            requireAllowed(request.url)
            val response = chain.proceed(request)
            if (!response.isRedirect) return response

            val location = response.headers["Location"]
            response.close()
            if (location.isNullOrBlank()) {
                throw IOException("Redirect response without a Location header")
            }
            if (redirects >= maxRedirects) {
                throw IOException("Too many redirects (limit $maxRedirects)")
            }
            redirects++

            val target = request.url.resolve(location)
                ?: throw IOException("Cannot resolve redirect location")
            requireAllowed(target)
            if (request.url.isHttps && !target.isHttps) {
                throw ForbiddenDestinationException("Redirect from https to http is not allowed")
            }
            request = buildRedirectRequest(request, target)
        }
    }

    private fun requireAllowed(url: HttpUrl) {
        if (!policy.isAllowed(url.toString())) {
            throw ForbiddenDestinationException("Destination is not allowed by the network policy")
        }
    }

    /**
     * Собирает запрос для следующего перехода.
     *
     * При смене origin заголовки авторизации и cookie не переносятся.
     */
    private fun buildRedirectRequest(request: okhttp3.Request, target: HttpUrl): okhttp3.Request {
        val builder = request.newBuilder().url(target)
        if (originOf(target) != originOf(request.url)) {
            builder.removeHeader("Authorization")
            builder.removeHeader("Cookie")
        }
        return builder.build()
    }

    private fun originOf(url: HttpUrl): String = "${url.scheme}://${url.host}:${url.port}"
}
