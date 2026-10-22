package architecture

import com.lemonappdev.konsist.api.Konsist
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Один тип — одно место в модуле.
 *
 * Два объявления с одинаковым именем внутри одного Gradle-модуля (например,
 * `api/GalleryRepository.kt` и `api/domain/GalleryRepository.kt`) означают, что
 * потребитель импортирует случайный из них, а сигнатуры расходятся молча.
 * Компилятор это пропускает: имена в разных пакетах легальны.
 *
 * Правило «всегда красный»: осознанное разделение имён не допускается —
 * переименуй один из типов, тогда связь будет видна в импортах.
 */
class DuplicateContractsTest {

    @Test
    fun `no duplicate top-level type names inside a module`() {
        val files = Konsist.scopeFromProduction().files
        Coverage.report("DuplicateContractsTest", "production-файлы", files.size)

        val duplicates = files
            .groupBy { file -> file.moduleName ?: "<без модуля>" }
            .flatMap { (module, moduleFiles) ->
                val names = moduleFiles.flatMap { file ->
                    file.classes(includeNested = false, includeLocal = false).map { it.name } +
                        file.interfaces(includeNested = false).map { it.name } +
                        file.objects(includeNested = false).map { it.name }
                }
                names
                    .groupingBy { name -> name }
                    .eachCount()
                    .filterValues { count -> count > 1 }
                    .keys
                    .sorted()
                    .map { name -> "$module → $name" }
            }
            .sorted()

        assertTrue(
            buildString {
                append("Дубли имён типов в одном модуле (")
                append(duplicates.size)
                append("). Один тип — одно место: переименуй или удали копию:\n")
                duplicates.forEach { duplicate -> append("  - ").append(duplicate).append('\n') }
            },
            duplicates.isEmpty(),
        )
    }
}
