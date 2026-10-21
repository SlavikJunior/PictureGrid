package com.example.picturegrid.feature.gallery.impl

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
 * Фоновая повторная синхронизация списка изображений.
 *
 * Worker не содержит собственной HTTP-логики: он вызывает тот же
 * [GalleryRepository], что и экран галереи. Уникальность работы и
 * ограничение по сети обеспечивают отсутствие дублирующих запросов.
 */
internal class GallerySyncWorker constructor(
    appContext: Context,
    params: WorkerParameters,
    private val repository: GalleryRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            syncGallery(repository)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        suspend fun syncGallery(repository: GalleryRepository): Result {
            return when (val result = repository.refresh()) {
                is GalleryRefreshResult.Success -> Result.success()
                is GalleryRefreshResult.Failure -> {
                    if (result.error.retryable) Result.retry() else Result.failure()
                }
            }
        }
    }
}

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
