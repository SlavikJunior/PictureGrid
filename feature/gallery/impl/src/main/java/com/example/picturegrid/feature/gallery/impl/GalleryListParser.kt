package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.core.network.PublicNetworkDestinationPolicy
import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.PlaceholderReason
import java.net.URI

/**
 * Результат разбора текста со списком URL.
 *
 * Разбор либо успешен, либо отклонён: список, нарушающий ограничения
 * формата, не обрезается молча и не подменяется частичным результатом.
 */
sealed interface GalleryListParseResult {

    /** Список разобран, ограничения соблюдены. */
    data class Parsed(val items: List<GalleryItem>) : GalleryListParseResult

    /** Список отклонён из-за нарушения ограничений. */
    data class Rejected(val reason: GalleryListRejection) : GalleryListParseResult
}

/** Причина отклонения списка. */
enum class GalleryListRejection {

    /** В списке больше элементов, чем допускает [GalleryListLimits.MAX_ITEMS]. */
    TOO_MANY_ITEMS,

    /** Одна из строк длиннее, чем допускает [GalleryListLimits.MAX_LINE_LENGTH]. */
    LINE_TOO_LONG,

    /** Объём текста превышает [GalleryListLimits.MAX_TEXT_BYTES]. */
    TEXT_TOO_LARGE,
}

/**
 * Разбор текста со списком URL.
 *
 * Пустые строки пропускаются. Валидные HTTP(S)-URL становятся
 * [GalleryItem.Image] (кандидатами в изображение; тип ресурса уточняется
 * при загрузке). Всё остальное — [GalleryItem.Placeholder] с причиной
 * [PlaceholderReason.NotUrl]. Тип ресурса не определяется по расширению.
 *
 * Разбор ограничен по числу элементов и длине строки: нарушение лимита
 * приводит к [GalleryListParseResult.Rejected], а не к усечённому списку.
 */
object GalleryListParser {

    fun parse(text: String): GalleryListParseResult {
        if (text.length > GalleryListLimits.MAX_TEXT_CHARS) {
            return GalleryListParseResult.Rejected(GalleryListRejection.TEXT_TOO_LARGE)
        }
        val occurrences = mutableMapOf<String, Int>()
        val items = mutableListOf<GalleryItem>()
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.length > GalleryListLimits.MAX_LINE_LENGTH) {
                return GalleryListParseResult.Rejected(GalleryListRejection.LINE_TOO_LONG)
            }
            if (items.size >= GalleryListLimits.MAX_ITEMS) {
                return GalleryListParseResult.Rejected(GalleryListRejection.TOO_MANY_ITEMS)
            }
            val occurrence = occurrences.getOrDefault(line, 0)
            occurrences[line] = occurrence + 1
            val id = if (occurrence == 0) line else "$line#$occurrence"
            items += toItem(id, line)
        }
        return GalleryListParseResult.Parsed(items)
    }

    private fun toItem(id: String, line: String): GalleryItem {
        val uri = runCatching { URI(line) }.getOrNull()
        val isHttpUrl = uri != null &&
            uri.isAbsolute &&
            uri.scheme?.lowercase() in setOf("http", "https") &&
            !uri.host.isNullOrBlank()
        return when {
            !isHttpUrl -> GalleryItem.Placeholder(id = id, source = line, reason = PlaceholderReason.NotUrl)
            // Адрес назначения проверяется до попадания в галерею: запрещённый
            // адрес не должен инициировать загрузку и повторные попытки.
            !PublicNetworkDestinationPolicy.isAllowed(line) -> GalleryItem.Placeholder(
                id = id,
                source = line,
                reason = PlaceholderReason.ForbiddenDestination,
            )
            else -> GalleryItem.Image(id = id, source = line)
        }
    }
}
