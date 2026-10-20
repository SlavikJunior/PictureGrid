package com.example.picturegrid.feature.gallery.impl

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.picturegrid.feature.gallery.api.GalleryError
import com.example.picturegrid.feature.gallery.api.GalleryItem
import com.example.picturegrid.feature.gallery.api.GalleryRefreshResult
import com.example.picturegrid.feature.gallery.api.GalleryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GallerySyncWorkerTest {

    private lateinit var context: Context
    private lateinit var workerFactory: WorkerFactory

    @Before
    fun setUp() {
        context = org.robolectric.RuntimeEnvironment.getApplication()
    }

    private fun initWorkManager(repository: GalleryRepository) {
        workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): androidx.work.ListenableWorker? {
                if (workerClassName == GallerySyncWorker::class.java.name) {
                    return GallerySyncWorker(appContext, workerParameters, repository)
                }
                return null
            }
        }
        val config = Configuration.Builder().setWorkerFactory(workerFactory).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun `worker returns success when refresh succeeds`() = runBlocking {
        val repository = FakeRepository(GalleryRefreshResult.Success(listOf(GalleryItem.Image("a", "https://example.com/a.png"))))
        initWorkManager(repository)
        val worker = TestListenableWorkerBuilder<GallerySyncWorker>(context).setWorkerFactory(workerFactory).build()
        assertEquals(androidx.work.ListenableWorker.Result.success(), worker.doWork())
    }

    @Test
    fun `worker requests retry on temporary network error`() = runBlocking {
        val repository = FakeRepository(
            GalleryRefreshResult.Failure(GalleryError.Network("timeout", retryable = true), null)
        )
        initWorkManager(repository)
        val worker = TestListenableWorkerBuilder<GallerySyncWorker>(context).setWorkerFactory(workerFactory).build()
        assertEquals(androidx.work.ListenableWorker.Result.retry(), worker.doWork())
    }

    @Test
    fun `worker returns failure on permanent error`() = runBlocking {
        val repository = FakeRepository(
            GalleryRefreshResult.Failure(GalleryError.Network("HTTP error 404", retryable = false), null)
        )
        initWorkManager(repository)
        val worker = TestListenableWorkerBuilder<GallerySyncWorker>(context).setWorkerFactory(workerFactory).build()
        assertEquals(androidx.work.ListenableWorker.Result.failure(), worker.doWork())
    }

    @Test
    fun `syncGallery maps cancellation`() = runBlocking {
        val repository = object : GalleryRepository {
            override suspend fun getCached(): List<GalleryItem>? = null
            override suspend fun refresh(): GalleryRefreshResult {
                throw kotlinx.coroutines.CancellationException("cancelled")
            }
        }
        try {
            GallerySyncWorker.syncGallery(repository)
            org.junit.Assert.fail("Expected CancellationException")
        } catch (e: kotlinx.coroutines.CancellationException) {
            // expected
        }
    }

    private class FakeRepository(
        private val result: GalleryRefreshResult,
    ) : GalleryRepository {
        override suspend fun getCached(): List<GalleryItem>? = null
        override suspend fun refresh(): GalleryRefreshResult = result
    }
}
