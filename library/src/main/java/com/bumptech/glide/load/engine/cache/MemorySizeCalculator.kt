package com.bumptech.glide.load.engine.cache

import android.annotation.TargetApi
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.text.format.Formatter
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Synthetic
import kotlin.math.roundToInt

/**
 * A calculator that tries to intelligently determine cache sizes for a given device based on some
 * constants and the devices screen density, width, and height.
 */
class MemorySizeCalculator internal constructor(builder: Builder) {
    /** Returns the recommended bitmap pool size for the device it is run on in bytes.  */
    var bitmapPoolSize = 0
    /** Returns the recommended memory cache size for the device it is run on in bytes.  */
    var memoryCacheSize = 0
    private val context: Context
    /** Returns the recommended array pool size for the device it is run on in bytes.  */
    val arrayPoolSizeInBytes: Int

    interface ScreenDimensions {
        val widthPixels: Int
        val heightPixels: Int
    }

    private fun toMb(bytes: Int): String {
        return Formatter.formatFileSize(context, bytes.toLong())
    }

    /**
     * Constructs an [MemorySizeCalculator] with reasonable defaults that can be optionally
     * overridden.
     */
    // Public API.
    class Builder(@field:Synthetic val context: Context) {
        // Modifiable (non-final) for testing.
        @Synthetic
        var activityManager: ActivityManager

        @Synthetic
        var screenDimensions: ScreenDimensions

        @Synthetic
        var memoryCacheScreens = MEMORY_CACHE_TARGET_SCREENS.toFloat()

        @Synthetic
        var bitmapPoolScreens = BITMAP_POOL_TARGET_SCREENS.toFloat()

        @Synthetic
        var maxSizeMultiplier = MAX_SIZE_MULTIPLIER

        @Synthetic
        var lowMemoryMaxSizeMultiplier = LOW_MEMORY_MAX_SIZE_MULTIPLIER

        @Synthetic
        var arrayPoolSizeBytes = ARRAY_POOL_SIZE_BYTES

        /**
         * Sets the number of device screens worth of pixels the [ ] should be able to hold and returns this
         * Builder.
         */
        fun setMemoryCacheScreens(memoryCacheScreens: Float): Builder {
            Preconditions.checkArgument(
                    memoryCacheScreens >= 0, "Memory cache screens must be greater than or equal to 0")
            this.memoryCacheScreens = memoryCacheScreens
            return this
        }

        /**
         * Sets the number of device screens worth of pixels the [ ] should be able to hold and returns
         * this Builder.
         */
        fun setBitmapPoolScreens(bitmapPoolScreens: Float): Builder {
            Preconditions.checkArgument(
                    bitmapPoolScreens >= 0, "Bitmap pool screens must be greater than or equal to 0")
            this.bitmapPoolScreens = bitmapPoolScreens
            return this
        }

        /**
         * Sets the maximum percentage of the device's memory class for standard devices that can be
         * taken up by Glide's [com.bumptech.glide.load.engine.cache.MemoryCache] and [ ] put together, and returns this
         * builder.
         */
        fun setMaxSizeMultiplier(maxSizeMultiplier: Float): Builder {
            Preconditions.checkArgument(
                    maxSizeMultiplier in 0.0..1.0,
                    "Size multiplier must be between 0 and 1")
            this.maxSizeMultiplier = maxSizeMultiplier
            return this
        }

        /**
         * Sets the maximum percentage of the device's memory class for low ram devices that can be
         * taken up by Glide's [com.bumptech.glide.load.engine.cache.MemoryCache] and [ ] put together, and returns this
         * builder.
         *
         * @see ActivityManager.isLowRamDevice
         */
        fun setLowMemoryMaxSizeMultiplier(lowMemoryMaxSizeMultiplier: Float): Builder {
            Preconditions.checkArgument(
                    lowMemoryMaxSizeMultiplier in 0.0..1.0,
                    "Low memory max size multiplier must be between 0 and 1")
            this.lowMemoryMaxSizeMultiplier = lowMemoryMaxSizeMultiplier
            return this
        }

        /**
         * Sets the size in bytes of the [com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool]
         * to use to store temporary arrays while decoding data and returns this builder.
         *
         *
         * This number will be halved on low memory devices that return `true` from [ ][ActivityManager.isLowRamDevice].
         */
        fun setArrayPoolSize(arrayPoolSizeBytes: Int): Builder {
            this.arrayPoolSizeBytes = arrayPoolSizeBytes
            return this
        }

        @VisibleForTesting
        fun setActivityManager(activityManager: ActivityManager): Builder {
            this.activityManager = activityManager
            return this
        }

        @VisibleForTesting
        fun setScreenDimensions(screenDimensions: ScreenDimensions): Builder {
            this.screenDimensions = screenDimensions
            return this
        }

        fun build(): MemorySizeCalculator {
            return MemorySizeCalculator(this)
        }

        companion object {
            @VisibleForTesting
            const val MEMORY_CACHE_TARGET_SCREENS = 2

            /**
             * On Android O+, we use [android.graphics.Bitmap.Config.HARDWARE] for all reasonably
             * sized images unless we're creating thumbnails for the first time. As a result, the Bitmap
             * pool is much less important on O than it was on previous versions.
             */
            @JvmField
            val BITMAP_POOL_TARGET_SCREENS = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) 4 else 1
            const val MAX_SIZE_MULTIPLIER = 0.4f
            const val LOW_MEMORY_MAX_SIZE_MULTIPLIER = 0.33f

            // 4MB.
            const val ARRAY_POOL_SIZE_BYTES = 4 * 1024 * 1024
        }

        init {
            activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            screenDimensions = DisplayMetricsScreenDimensions(context.resources.displayMetrics)

            // On Android O+ Bitmaps are allocated natively, ART is much more efficient at managing
            // garbage and we rely heavily on HARDWARE Bitmaps, making Bitmap re-use much less important.
            // We prefer to preserve RAM on these devices and take the small performance hit of not
            // re-using Bitmaps and textures when loading very small images or generating thumbnails.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isLowMemoryDevice(activityManager)) {
                bitmapPoolScreens = 0f
            }
        }
    }

    private class DisplayMetricsScreenDimensions internal constructor(private val displayMetrics: DisplayMetrics) : ScreenDimensions {
        override val widthPixels: Int
            get() = displayMetrics.widthPixels
        override val heightPixels: Int
            get() = displayMetrics.heightPixels
    }

    companion object {
        private const val TAG = "MemorySizeCalculator"

        @VisibleForTesting
        const val BYTES_PER_ARGB_8888_PIXEL = 4
        private const val LOW_MEMORY_BYTE_ARRAY_POOL_DIVISOR = 2
        private fun getMaxSize(
                activityManager: ActivityManager, maxSizeMultiplier: Float, lowMemoryMaxSizeMultiplier: Float): Int {
            val memoryClassBytes = activityManager.memoryClass * 1024 * 1024
            val isLowMemoryDevice = isLowMemoryDevice(activityManager)
            return (memoryClassBytes * if (isLowMemoryDevice) lowMemoryMaxSizeMultiplier else maxSizeMultiplier).roundToInt()
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Synthetic
        fun isLowMemoryDevice(activityManager: ActivityManager): Boolean {
            // Explicitly check with an if statement, on some devices both parts of boolean expressions
            // can be evaluated even if we'd normally expect a short circuit.
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                activityManager.isLowRamDevice
            } else {
                true
            }
        }
    }

    // Package private to avoid PMD warning.
    init {
        context = builder.context
        arrayPoolSizeInBytes = if (isLowMemoryDevice(builder.activityManager)) builder.arrayPoolSizeBytes / LOW_MEMORY_BYTE_ARRAY_POOL_DIVISOR else builder.arrayPoolSizeBytes
        val maxSize = getMaxSize(
                builder.activityManager, builder.maxSizeMultiplier, builder.lowMemoryMaxSizeMultiplier)
        val widthPixels = builder.screenDimensions.widthPixels
        val heightPixels = builder.screenDimensions.heightPixels
        val screenSize = widthPixels * heightPixels * BYTES_PER_ARGB_8888_PIXEL
        val targetBitmapPoolSize = (screenSize * builder.bitmapPoolScreens).roundToInt()
        val targetMemoryCacheSize = (screenSize * builder.memoryCacheScreens).roundToInt()
        val availableSize = maxSize - arrayPoolSizeInBytes
        if (targetMemoryCacheSize + targetBitmapPoolSize <= availableSize) {
            memoryCacheSize = targetMemoryCacheSize
            bitmapPoolSize = targetBitmapPoolSize
        } else {
            val part = availableSize / (builder.bitmapPoolScreens + builder.memoryCacheScreens)
            memoryCacheSize = (part * builder.memoryCacheScreens).roundToInt()
            bitmapPoolSize = (part * builder.bitmapPoolScreens).roundToInt()
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(
                    TAG,
                    "Calculation complete"
                            + ", Calculated memory cache size: "
                            + toMb(memoryCacheSize)
                            + ", pool size: "
                            + toMb(bitmapPoolSize)
                            + ", byte array size: "
                            + toMb(arrayPoolSizeInBytes)
                            + ", memory class limited? "
                            + (targetMemoryCacheSize + targetBitmapPoolSize > maxSize)
                            + ", max size: "
                            + toMb(maxSize)
                            + ", memoryClass: "
                            + builder.activityManager.memoryClass
                            + ", isLowMemoryDevice: "
                            + isLowMemoryDevice(builder.activityManager))
        }
    }
}