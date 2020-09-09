package com.bumptech.glide.load.engine.bitmap_recycle

import android.graphics.Bitmap

/**
 * An [BitmapPool][com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool] implementation
 * that rejects all [Bitmap][android.graphics.Bitmap]s added to it and always returns `null` from get.
 */
open class BitmapPoolAdapter : BitmapPool {
    override val maxSize: Long
        get() = 0

    override fun setSizeMultiplier(sizeMultiplier: Float) {
        // Do nothing.
    }

    override fun put(bitmap: Bitmap?) {
        bitmap?.recycle()
    }

    override fun get(width: Int, height: Int, config: Bitmap.Config?): Bitmap {
        return Bitmap.createBitmap(width, height, config!!)
    }

    override fun getDirty(width: Int, height: Int, config: Bitmap.Config?): Bitmap {
        return get(width, height, config)
    }

    override fun clearMemory() {
        // Do nothing.
    }

    override fun trimMemory(level: Int) {
        // Do nothing.
    }
}