package com.example.picturegrid.core.images

import androidx.compose.runtime.staticCompositionLocalOf
import coil3.ImageLoader

/**
 * Единственный [ImageLoader] приложения, доступный UI-слою.
 *
 * Загрузчик — общий инфраструктурный сервис, а не состояние экрана: держать
 * его в параметрах каждой Composable-функции значит протаскивать одну и ту
 * же зависимость через всю иерархию. Провайдер задаётся один раз в
 * composition root приложения.
 *
 * Значение статическое: подмена загрузчика во время работы не ожидается.
 * Если провайдер забыт, ошибка возникает сразу при первом обращении, а не
 * превращается в тихий дубль кэша.
 */
val LocalImageLoader = staticCompositionLocalOf<ImageLoader> {
    error("LocalImageLoader не предоставлен: оберните UI в CompositionLocalProvider")
}
