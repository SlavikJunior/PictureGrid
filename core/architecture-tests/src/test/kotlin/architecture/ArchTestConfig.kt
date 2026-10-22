package architecture

/**
 * Общие настройки архитектурных тестов.
 *
 * Меняется в одном месте — здесь — при смене структуры пакетов проекта.
 */

/**
 * Корневой пакет проекта (совпадает с namespace/applicationId).
 * Меняется в одном месте при смене идентификаторов приложения.
 */
const val BASE_PACKAGE = "com.example.picturegrid"

/**
 * Пакеты слоёв. Используется wildcard-паттерн `..domain..`, поэтому
 * правило работает независимо от корневого пакета проекта
 * (`com.example.feature.cart.domain` и `com.example.domain` — оба подходят).
 */
const val DOMAIN_PACKAGE = "..domain.."
const val DATA_PACKAGE = "..data.."
const val PRESENTATION_PACKAGE = "..presentation.."

/**
 * Префиксы пакетов, запрещённые в слое `domain` (INV-05, INV-06).
 *
 * Только очевидные нарушения — чтобы правило не давало ложных срабатываний.
 * Всё остальное проверяется ревью.
 */
val FORBIDDEN_IN_DOMAIN = listOf(
    "android.",
    "java.",
    "javax.",
    "androidx.room.",
    "androidx.compose.",
    "androidx.navigation.",
    "androidx.lifecycle.",
    "androidx.activity.",
    "androidx.fragment.",
    "io.ktor.",
    "retrofit2.",
    "okhttp3.",
    "org.koin.",
    "javax.inject",
    "dagger.",
    "com.google.dagger.",
    "kotlinx.serialization.",
)

/**
 * Аннотации, запрещённые в слое `domain` (INV-06).
 *
 * `domain` описывает бизнес-правила и не знает, как данные хранятся и передаются.
 * Импорт ловится через [FORBIDDEN_IN_DOMAIN], аннотация — здесь: она может быть
 * применена без полного имени пакета (например, через `typealias`).
 */
val FORBIDDEN_ANNOTATIONS_IN_DOMAIN = listOf(
    "Serializable",
    "SerialName",
    "Entity",
    "Embedded",
    "Relation",
    "Parcelize",
    "Inject",
)

/**
 * Единственные публичные объявления в `:feature:X:impl` (INV-27).
 *
 * `NavEntry` — контракт навигации модуля. `Module` — Koin-модуль фичи в проектах
 * без `@ComponentScan`; если DI собирается сканированием пакетов, оставь только
 * `NavEntry`.
 */
val ALLOWED_PUBLIC_SUFFIXES = listOf("NavEntry", "Module")

/**
 * Пакеты модуля `impl`, в которых разрешено держать файлы напрямую (INV-25).
 *
 * Всё остальное — в подпакетах `presentation/`, `data/`, `navigation/`, `di/`.
 */
const val IMPL_PACKAGE_SUFFIX = ".impl"

/** Файлы-точки входа, которым можно лежать в корне пакета `impl`. */
val ALLOWED_IMPL_ROOT_FILES = listOf("Module", "NavEntry")

/**
 * Супертип, который делает тип ключом навигации Navigation 3 (INV-18).
 *
 * `Route` — на случай проектного алиаса (`typealias Route = NavKey` или
 * собственный маркерный интерфейс). Если в проекте такого нет — оставь только
 * `NavKey`: тогда тест строже.
 */
val NAV_KEY_TYPES = listOf("NavKey", "Route")

/**
 * Способы достать зависимость из контейнера мимо конструктора (INV-22).
 *
 * Разрешены только в точке входа (`Activity`, `Application`) и в Route-слое
 * (`*Screen.kt`). В `domain` и `data` их быть не может.
 */
val FORBIDDEN_LOCATOR_CALLS = listOf(
    "KoinComponent",
    "getKoin(",
    "GlobalContext",
    "koinInject",
    "EntryPoints.get",
    "EntryPointAccessors",
)

/**
 * Суффиксы типов, которые держат состояние экрана.
 *
 * В проекте состояние держит MVIKotlin `Store`, а не ViewModel (см. отклонение
 * INV-08 в `docs/PROFILE.md`). Проверка контракта состояния направлена на оба
 * типа: иначе она смотрит в пустоту и зеленеет, никого не проверив.
 */
val STATE_HOLDER_SUFFIXES = listOf("ViewModel", "Store")

/** Суффиксы, по которым определяется роль типа. */
object Suffixes {
    const val VIEW_MODEL = "ViewModel"
    const val UI_STATE = "UiState"
    const val UI_EVENT = "UiEvent"
    const val UI_EFFECT = "UiEffect"
    const val REPOSITORY = "Repository"
    const val USE_CASE = "UseCase"
    const val SCREEN = "Screen"
    const val CONTENT = "Content"
    const val DTO = "Dto"
    const val ENTITY = "Entity"
}
