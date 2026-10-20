# PictureGrid

Android-приложение для загрузки списка URL изображений, показа адаптивной галереи миниатюр и просмотра оригиналов в BottomSheet.

## Архитектура

Проект состоит из модулей:

- `:app` — composition root, навигация, тема.
- `:feature:gallery:api` / `:feature:gallery:impl` — контракт и реализация галереи.
- `:feature:viewer:api` / `:feature:viewer:impl` — контракт и реализация просмотра оригинала.
- `:core:common` — общие типы.
- `:core:network` — HTTP-клиент, ошибки, retry-классификация.
- `:core:images` — Coil `ImageLoader`, `ImageStorage`, загрузка оригиналов.
- `:core:navigation` — Router-обёртка над Navigation 3 и BottomSheet strategy.
- `:core:designsystem` — светлая и тёмная темы.

Архитектура feature-based: `api` не зависит от `impl`, `core` не зависит от конкретных feature-модулей. UI использует MVIKotlin с однонаправленным потоком данных.

## Запуск

Сборка debug APK:

```bash
./gradlew assembleDebug
```

Установка на устройство/эмулятор:

```bash
./gradlew installDebug
```

Unit-тесты:

```bash
./gradlew testDebugUnitTest
```

Lint:

```bash
./gradlew lintDebug
```

Проверка зависимостей модулей:

```bash
./gradlew checkModuleDependencies
```

Screenshot-эталоны Roborazzi:

```bash
./gradlew verifyRoborazziDebug
```

## Поддерживаемые версии Android

- `minSdk = 26` (Android 8.0)
- `targetSdk = 37`
- `compileSdk = 37`

Диапазон подтверждён по фактической конфигурации проекта и совместимости используемых зависимостей.

## Кэширование

- Coil использует один общий `ImageLoader` с явными memory cache и disk cache.
- Превью Coil являются UI-кэшем и могут вытесняться политикой cache.
- Оригиналы сохраняются отдельно через `ImageStorage` в приватном каталоге приложения и читаются с диска при повторном открытии и после перезапуска.
- `images.txt` сохраняется в приватном каталоге приложения, а не в исходниках проекта.

## Retry и сеть

- Список загружается автоматически при запуске.
- При сетевой ошибке сохранённая копия не затирается.
- Повторная синхронизация запускается через WorkManager с ограничением `NetworkType.CONNECTED`.
- Retry-политика определяется `NetworkError.isRetryable()`: временные сетевые ошибки, timeout, `EmptyBody`, HTTP 408/429/5xx повторяются; остальные 4xx не повторяются.
- Ошибки хранилища не считаются retryable.

## Известные ограничения

- Single-flight для параллельных `refresh()` не реализован.
- Бонусные функции (zoom, swipe, immersive mode, sharing) не входят в MVP.
- Instrumentation-тесты требуют запущенного устройства/эмулятора.
