package com.example.picturegrid.feature.gallery.impl

import android.content.Context
import com.example.picturegrid.core.network.RetrofitNetworkClient
import com.example.picturegrid.feature.gallery.api.GalleryRepository
import com.example.picturegrid.feature.gallery.impl.GalleryListStorage
import com.example.picturegrid.feature.gallery.impl.GalleryRepositoryImpl
import java.io.File

internal fun createDefaultRepository(context: Context): GalleryRepository {
    val storage = GalleryListStorage(File(context.filesDir, "gallery/list.txt"))
    return GalleryRepositoryImpl(
        networkClient = RetrofitNetworkClient(),
        storage = storage,
    )
}
