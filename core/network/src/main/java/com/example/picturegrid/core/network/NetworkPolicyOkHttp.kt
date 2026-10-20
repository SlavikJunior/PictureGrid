package com.example.picturegrid.core.network

import okhttp3.OkHttpClient

/**
 * Подключает политику сетевых назначений к OkHttp-клиенту.
 *
 * Клиент должен использовать этот метод и в сетевом слое приложения, и в
 * клиенте Coil: у них разные экземпляры [okhttp3.OkHttpClient], поэтому
 * защита одного не распространяется на другой.
 */
fun OkHttpClient.Builder.applyNetworkPolicy(
    policy: NetworkDestinationPolicy = PublicNetworkDestinationPolicy,
    maxRedirects: Int = NetworkDestinationPolicy.MAX_REDIRECTS,
): OkHttpClient.Builder =
    apply {
        // Редиректы обрабатываются вручную, чтобы каждая цель проверялась
        // до отправки запроса.
        followRedirects(false)
        followSslRedirects(false)
        dns(SafeDns(policy))
        addInterceptor(NetworkPolicyInterceptor(policy, maxRedirects))
    }
