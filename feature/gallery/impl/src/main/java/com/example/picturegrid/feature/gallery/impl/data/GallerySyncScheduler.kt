package com.example.picturegrid.feature.gallery.impl.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.example.picturegrid.feature.gallery.api.domain.GalleryRepository
import com.example.picturegrid.feature.gallery.api.domain.GalleryRefreshResult
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

/**
 * Планировщик уникальной фоновой синхронизации.
 */
internal class GallerySyncScheduler(
    private val workManager: WorkManager,
) {
    companion object {
        const val UNIQUE_WORK_NAME = "gallery_list_sync"
    }

    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<GallerySyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS,
            )
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
