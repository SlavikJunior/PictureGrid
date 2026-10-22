package com.example.picturegrid.feature.gallery.impl.presentation.state

import com.example.picturegrid.feature.gallery.api.domain.GalleryItem
import com.example.picturegrid.feature.gallery.api.domain.GalleryRefreshResult
import com.example.picturegrid.feature.gallery.api.domain.GalleryRepository
import com.example.picturegrid.feature.gallery.api.domain.PlaceholderReason
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryStoreSmokeTest {
    @Test
    fun `placeholder reasons compile`() {
        val p = GalleryItem.Placeholder("id", "src", PlaceholderReason.LoadFailed)
        assertEquals(PlaceholderReason.LoadFailed, p.reason)
    }
}
