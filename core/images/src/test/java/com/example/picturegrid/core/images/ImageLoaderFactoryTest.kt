package com.example.picturegrid.core.images

import android.content.Context
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageLoaderFactoryTest {

    @Test
    fun `creates loader with explicit memory and disk caches`() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val loader = ImageLoaderFactory.create(context)

        assertNotNull(loader.memoryCache)
        assertNotNull(loader.diskCache)
        assertTrue(loader.diskCache!!.directory.toString().contains("coil_image_cache"))
    }
}
