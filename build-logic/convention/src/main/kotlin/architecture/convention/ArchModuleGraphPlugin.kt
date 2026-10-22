package architecture.convention

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Проверяет направления зависимостей между модулями.
 *
 * Реализует инварианты стандарта:
 *   INV-01 — фичи не зависят друг от друга; фича не зависит от :app
 *   INV-02 — feature:X:api не зависит от feature:X:impl
 *   INV-03 — core:* не зависит от feature:* и :app
 *
 * Запуск: ./gradlew checkModuleDependencies
 *
 * Внимание, два нюанса Gradle API в Kotlin:
 *
 * 1. У `allprojects`, `afterEvaluate`, `register`, `configure` есть перегрузка с
 *    `groovy.lang.Closure`. Kotlin не может выбрать между ней и `Action<? super T>`
 *    и падает с «but '(Closure<Any>..Closure<*>)' was expected».
 *    Поэтому всегда пишем `Action<T> { ... }` явно.
 *
 * 2. Плагин `kotlin-dsl` подключает `SamWithReceiver`, поэтому лямбда для
 *    `Action<T>` пишется в стиле receiver: параметр не объявляется, объект
 *    доступен как `this`. Форма `Action<T> { value -> ... }` не компилируется.
 */
class ArchModuleGraphPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val root = target.rootProject
        val collectedEdges = LinkedHashSet<String>()

        // Собираем прямые зависимости вида "проект → проект".
        // afterEvaluate: к моменту срабатывания build-скрипт модуля уже выполнен.
        root.allprojects(
            Action<Project> {
                afterEvaluate(
                    Action<Project> {
                        configurations.forEach { configuration ->
                            configuration.dependencies
                                .filterIsInstance<ProjectDependency>()
                                .forEach { dependency ->
                                    collectedEdges += "$path -> ${dependency.path}"
                                }
                        }
                    }
                )
            }
        )

        root.tasks.register(
            "checkModuleDependencies",
            CheckModuleDependencies::class.java,
            Action<CheckModuleDependencies> {
                group = "verification"
                description =
                    "Проверяет направления зависимостей между модулями (INV-01, INV-02, INV-03)"
            }
        )

        root.tasks.register(
            "printModuleGraph",
            PrintModuleGraph::class.java,
            Action<PrintModuleGraph> {
                group = "help"
                description = "Печатает граф зависимостей модулей"
            }
        )

        // Материализуем граф в обычные строки до конца конфигурации —
        // так задачи остаются совместимыми с configuration cache.
        root.gradle.projectsEvaluated(
            Action<Gradle> {
                root.tasks
                    .named("checkModuleDependencies", CheckModuleDependencies::class.java)
                    .configure(Action<CheckModuleDependencies> { edges.set(collectedEdges.toList()) })
                root.tasks
                    .named("printModuleGraph", PrintModuleGraph::class.java)
                    .configure(Action<PrintModuleGraph> { edges.set(collectedEdges.toList()) })
            }
        )
    }
}

abstract class CheckModuleDependencies : DefaultTask() {

    @get:Input
    abstract val edges: ListProperty<String>

    @TaskAction
    fun check() {
        val graph = buildGraph(edges.get())
        val violations = mutableListOf<String>()

        graph.forEach { (from, targets) ->
            targets.forEach { to ->
                ModuleRules.violation(from, to)?.let { violations += it }
            }
        }
        violations += ModuleRules.cycles(graph)

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Нарушения архитектурного стандарта (${violations.size}):")
                    violations.forEach { appendLine("  - $it") }
                    appendLine()
                    appendLine("Подробнее: docs/PROFILE.md → раздел «Отклонения от стандарта».")
                }
            )
        }

        logger.lifecycle(
            "Граф модулей корректен: ${graph.size} модулей с зависимостями, " +
                "${edges.get().size} зависимостей.",
        )
    }

    private fun buildGraph(rawEdges: List<String>): Map<String, Set<String>> =
        rawEdges
            .map { it.split(" -> ") }
            .groupBy({ it[0] }, { it[1] })
            .mapValues { (_, values) -> values.toSet() }
}

abstract class PrintModuleGraph : DefaultTask() {

    @get:Input
    abstract val edges: ListProperty<String>

    @TaskAction
    fun print() {
        val graph = edges.get()
            .map { it.split(" -> ") }
            .groupBy({ it[0] }, { it[1] })
            .mapValues { (_, values) -> values.toSet() }

        graph.keys.sorted().forEach { module ->
            val targets = graph[module].orEmpty().sorted()
            logger.lifecycle(module)
            targets.forEach { logger.lifecycle("  ├── $it") }
        }
    }
}

/**
 * Правила графа модулей. Каждое нарушение ссылается на INV-ID из стандарта.
 */
object ModuleRules {

    private const val APP = ":app"
    private const val FEATURE = "feature"
    private const val CORE = "core"
    private val API_SUFFIXES = setOf("api")
    private val IMPL_SUFFIXES = setOf("impl", "internal")

    fun violation(from: String, to: String): String? {
        val fromFeature = featureName(from)
        val toFeature = featureName(to)

        // INV-02: api не зависит от impl
        if (fromFeature != null && toFeature != null &&
            fromFeature == toFeature &&
            lastSegment(from) in API_SUFFIXES &&
            lastSegment(to) in IMPL_SUFFIXES
        ) {
            return "$from -> $to нарушает INV-02: api-модуль не должен зависеть от impl-модуля."
        }

        // INV-01: разные фичи не зависят друг от друга
        if (fromFeature != null && toFeature != null && fromFeature != toFeature) {
            return "$from -> $to нарушает INV-01: фичи не должны зависеть друг от друга. " +
                "Общий контракт переносится в core."
        }

        // INV-01: фича не зависит от composition root
        if (fromFeature != null && to == APP) {
            return "$from -> $to нарушает INV-01: фича не должна зависеть от :app."
        }

        // INV-03: core не зависит от feature и app
        if (isCore(from) && (toFeature != null || to == APP)) {
            return "$from -> $to нарушает INV-03: core-модуль не должен зависеть от фичи или :app."
        }

        return null
    }

    fun cycles(graph: Map<String, Set<String>>): List<String> {
        val found = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val stack = mutableListOf<String>()

        fun visit(node: String) {
            if (node in stack) {
                val cycle = stack.dropWhile { it != node } + node
                found += "Цикл зависимостей (INV-01): ${cycle.joinToString(" -> ")}"
                return
            }
            if (!visited.add(node)) return

            stack += node
            graph[node].orEmpty().forEach { visit(it) }
            stack.removeAt(stack.lastIndex)
        }

        graph.keys.forEach { visit(it) }
        return found.distinct()
    }

    /** Имя фичи для путей вида :feature:cart и :feature:cart:impl. */
    private fun featureName(path: String): String? {
        val segments = path.removePrefix(":").split(":")
        val index = segments.indexOf(FEATURE)
        if (index < 0 || index + 1 >= segments.size) return null
        return segments[index + 1]
    }

    private fun isCore(path: String): Boolean {
        val segments = path.removePrefix(":").split(":")
        return segments.size >= 2 && segments[0] == CORE
    }

    private fun lastSegment(path: String): String =
        path.removePrefix(":").split(":").last()
}
