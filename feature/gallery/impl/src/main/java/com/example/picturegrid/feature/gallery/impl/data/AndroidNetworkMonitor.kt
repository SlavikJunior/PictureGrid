package com.example.picturegrid.feature.gallery.impl.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.picturegrid.feature.gallery.api.NetworkMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Реализация [NetworkMonitor] на [ConnectivityManager].
 *
 * Монитор нужен, чтобы экран узнал о восстановлении сети: фоновая
 * синхронизация обновляет сохранённый список, но без сигнала в UI список на
 * экране остался бы прежним.
 */
internal class AndroidNetworkMonitor(context: Context) : NetworkMonitor {

    private val connectivityManager: ConnectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override val isAvailable: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(hasInternet())
            }

            override fun onLost(network: Network) {
                trySend(hasInternet())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(hasInternet())
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        // Первое значение — текущее состояние сети: подписчик сразу знает,
        // доступна ли она, ещё до первого события.
        trySend(hasInternet())
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    /**
     * Проверяет активную сеть: события колбэка приходят и для сетей без
     * доступа в интернет, поэтому состояние пересчитывается.
     */
    private fun hasInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
