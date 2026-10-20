package com.example.picturegrid.feature.gallery.impl

import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.GalleryRefreshResult
import com.example.picturegrid.feature.gallery.api.GalleryRepository
import com.example.picturegrid.feature.gallery.api.PlaceholderReason
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryStoreSmokeTest {
    @Test
    fun `placeholder reasons compile`() {
        val p = GalleryItem.Placeholder("id", "src", PlaceholderReason.LoadFailed)
        assertEquals(PlaceholderReason.LoadFailed, p.reason)
    }
}
