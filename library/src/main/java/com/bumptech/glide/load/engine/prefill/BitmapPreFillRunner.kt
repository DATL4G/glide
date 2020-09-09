package com.bumptech.glide.load.engine.prefill

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.engine.cache.MemoryCache
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.util.Synthetic
import com.bumptech.glide.util.Util
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * A class that allocates [Bitmaps][android.graphics.Bitmap] to make sure that the [ ] is pre-populated.
 *
 *
 * By posting to the main thread with backoffs, we try to avoid ANRs when the garbage collector
 * gets into a state where a high percentage of [Bitmap] allocations trigger a stop the world
 * GC. We try to detect whether or not a GC has occurred by only allowing our allocator to run for a
 * limited number of milliseconds. Since the allocations themselves very fast, a GC is the most
 * likely reason for a substantial delay. If we detect our allocator has run for more than our
 * limit, we assume a GC has occurred, stop the current allocations, and try again after a delay.
 */
internal class BitmapPreFillRunner @VisibleForTesting constructor(
        private val bitmapPool: BitmapPool,
        private val memoryCache: MemoryCache,
        private val toPrefill: PreFillQueue,
        private val clock: Clock,
        private val handler: Handler) : Runnable {
    private val seenTypes: MutableSet<PreFillType> = HashSet()
    private var currentDelay = INITIAL_BACKOFF_MS
    private var isCancelled = false

    // Public API.
    constructor(
            bitmapPool: BitmapPool, memoryCache: MemoryCache, allocationOrder: PreFillQueue) : this(
            bitmapPool,
            memoryCache,
            allocationOrder,
            DEFAULT_CLOCK,
            Handler(Looper.getMainLooper())) {
    }

    fun cancel() {
        isCancelled = true
    }

    /**
     * Attempts to allocate [android.graphics.Bitmap]s and returns `true` if there are
     * more [android.graphics.Bitmap]s to allocate and `false` otherwise.
     */
    @VisibleForTesting
    fun allocate(): Boolean {
        val start = clock.now()
        while (!toPrefill.isEmpty && !isGcDetected(start)) {
            val toAllocate = toPrefill.remove()
            val bitmap: Bitmap
            bitmap = if (!seenTypes.contains(toAllocate)) {
                seenTypes.add(toAllocate)
                bitmapPool.getDirty(
                        toAllocate.width, toAllocate.height, toAllocate.config)
            } else {
                Bitmap.createBitmap(
                        toAllocate.width, toAllocate.height, toAllocate.config)
            }

            // Order matters here! If the Bitmap is too large or the BitmapPool is too full, it may be
            // recycled after the call to bitmapPool#put below.
            val bitmapSize = Util.getBitmapByteSize(bitmap)

            // Don't over fill the memory cache to avoid evicting useful resources, but make sure it's
            // not empty so that we use all available space.
            if (freeMemoryCacheBytes >= bitmapSize) {
                // We could probably make UniqueKey just always return false from equals,
                // but the allocation of the Key is not nearly as expensive as the allocation of the Bitmap,
                // so it's probably not worth it.
                val uniqueKey: Key = UniqueKey()
                memoryCache.put(uniqueKey, BitmapResource.obtain(bitmap, bitmapPool))
            } else {
                bitmapPool.put(bitmap)
            }
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(
                        TAG,
                        "allocated ["
                                + toAllocate.width
                                + "x"
                                + toAllocate.height
                                + "] "
                                + toAllocate.config
                                + " size: "
                                + bitmapSize)
            }
        }
        return !isCancelled && !toPrefill.isEmpty
    }

    private fun isGcDetected(startTimeMs: Long): Boolean {
        return clock.now() - startTimeMs >= MAX_DURATION_MS
    }

    private val freeMemoryCacheBytes: Long
        get() = memoryCache.maxSize - memoryCache.currentSize

    override fun run() {
        if (allocate()) {
            handler.postDelayed(this, nextDelay)
        }
    }

    private val nextDelay: Long
        get() {
            val result = currentDelay
            currentDelay = min(currentDelay * BACKOFF_RATIO, MAX_BACKOFF_MS)
            return result
        }

    private class UniqueKey @Synthetic internal constructor() : Key {
        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            throw UnsupportedOperationException()
        }
    }

    @VisibleForTesting
    internal class Clock {
        fun now(): Long {
            return SystemClock.currentThreadTimeMillis()
        }
    }

    companion object {
        @JvmField
        @VisibleForTesting
        val TAG = "PreFillRunner"
        private val DEFAULT_CLOCK = Clock()

        /**
         * The maximum number of millis we can run before posting. Set to match and detect the duration of
         * non concurrent GCs.
         */
        const val MAX_DURATION_MS: Long = 32

        /**
         * The amount of time in ms we wait before continuing to allocate after the first GC is detected.
         */
        const val INITIAL_BACKOFF_MS: Long = 40

        /** The amount by which the current backoff time is multiplied each time we detect a GC.  */
        const val BACKOFF_RATIO = 4

        /** The maximum amount of time in ms we wait before continuing to allocate.  */
        @JvmField
        val MAX_BACKOFF_MS = TimeUnit.SECONDS.toMillis(1)
    }
}