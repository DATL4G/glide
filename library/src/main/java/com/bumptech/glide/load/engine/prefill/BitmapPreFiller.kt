package com.bumptech.glide.load.engine.prefill

import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.engine.cache.MemoryCache
import com.bumptech.glide.util.Util
import java.util.*
import kotlin.math.roundToInt

/**
 * A class for pre-filling [Bitmaps][android.graphics.Bitmap] in a [ ].
 */
class BitmapPreFiller(
        private val memoryCache: MemoryCache, private val bitmapPool: BitmapPool, private val defaultFormat: DecodeFormat) {
    private var current: BitmapPreFillRunner? = null

    @Suppress("UNCHECKED_CAST")
    fun preFill(vararg bitmapAttributeBuilders: PreFillType.Builder) {
        current?.cancel()
        val bitmapAttributes = arrayOfNulls<PreFillType>(bitmapAttributeBuilders.size)
        for (i in bitmapAttributeBuilders.indices) {
            val builder = bitmapAttributeBuilders[i]
            if (builder.config == null) {
                builder.setConfig(
                        if (defaultFormat == DecodeFormat.PREFER_ARGB_8888) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565)
            }
            bitmapAttributes[i] = builder.build()
        }
        val allocationOrder = generateAllocationOrder(*bitmapAttributes as Array<out PreFillType>)
        current = BitmapPreFillRunner(bitmapPool, memoryCache, allocationOrder)
        Util.postOnUiThread(current)
    }

    @VisibleForTesting
    private fun generateAllocationOrder(vararg preFillSizes: PreFillType): PreFillQueue {
        val maxSize = memoryCache.maxSize - memoryCache.currentSize + bitmapPool.maxSize
        var totalWeight = 0
        for (size in preFillSizes) {
            totalWeight += size.weight
        }
        val bytesPerWeight = maxSize / totalWeight.toFloat()
        val attributeToCount: MutableMap<PreFillType, Int> = HashMap()
        for (size in preFillSizes) {
            val bytesForSize = (bytesPerWeight * size.weight).roundToInt()
            val bytesPerBitmap = getSizeInBytes(size)
            val bitmapsForSize = bytesForSize / bytesPerBitmap
            attributeToCount[size] = bitmapsForSize
        }
        return PreFillQueue(attributeToCount)
    }

    companion object {
        private fun getSizeInBytes(size: PreFillType): Int {
            return Util.getBitmapByteSize(size.width, size.height, size.config)
        }
    }
}