package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.PlaceholderReason
import java.net.URI

/**
 * Разбор текста со списком URL.
 *
 * Пустые строки пропускаются. Валидные HTTP(S)-URL становятся
 * [GalleryItem.Image] (кандидатами в изображение; тип ресурса уточняется
 * при загрузке). Всё остальное — [GalleryItem.Placeholder] с причиной
 * [PlaceholderReason.NotUrl]. Тип ресурса не определяется по расширению.
 *
 * Порядок строк сохраняется, повторяющиеся строки получают разные
 * идентификаторы. Объём текста ограничен транспортным лимитом
 * (см. `com.example.picturegrid.core.network.NetworkLimits`), поэтому
 * отдельных ограничений на число строк и их длину не требуется.
 */
object GalleryListParser {

    fun parse(text: String): List<GalleryItem> {
        val occurrences = mutableMapOf<String, Int>()
        val items = mutableListOf<GalleryItem>()
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val occurrence = occurrences.getOrDefault(line, 0)
            occurrences[line] = occurrence + 1
            val id = if (occurrence == 0) line else "$line#$occurrence"
            items += toItem(id, line)
        }
        return items
    }

    private fun toItem(id: String, line: String): GalleryItem {
        val uri = runCatching { URI(line) }.getOrNull()
        val isHttpUrl = uri != null &&
            uri.isAbsolute &&
            uri.scheme?.lowercase() in setOf("http", "https") &&
            !uri.host.isNullOrBlank()
        return if (isHttpUrl) {
            GalleryItem.Image(id = id, source = line)
        } else {
            GalleryItem.Placeholder(id = id, source = line, reason = PlaceholderReason.NotUrl)
        }
    }
}
