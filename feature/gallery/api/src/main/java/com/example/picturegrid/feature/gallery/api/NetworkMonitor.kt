package com.example.picturegrid.feature.gallery.api

import kotlinx.coroutines.flow.Flow

/**
 * Доступность сети для галереи.
 *
 * `true` означает, что есть активная сеть с доступом в интернет. Поток
 * cold: наблюдение начинается при подписке и останавливается при отписке.
 *
 * Контракт живёт в api-модуле: экран галереи принимает монитор снаружи,
 * поэтому потребитель должен видеть его тип.
 */
interface NetworkMonitor {
    val isAvailable: Flow<Boolean>
}
