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
 */
object GalleryListParser {

    fun parse(text: String): List<GalleryItem> {
        val occurrences = mutableMapOf<String, Int>()
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val occurrence = occurrences.getOrDefault(line, 0)
                occurrences[line] = occurrence + 1
                val id = if (occurrence == 0) line else "$line#$occurrence"
                toItem(id, line)
            }
            .toList()
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
