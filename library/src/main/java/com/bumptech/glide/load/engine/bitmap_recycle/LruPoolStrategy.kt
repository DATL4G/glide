package com.bumptech.glide.load.engine.bitmap_recycle

import android.graphics.Bitmap

internal interface LruPoolStrategy {
    fun put(bitmap: Bitmap)
    operator fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap?
    fun removeLast(): Bitmap?
    fun logBitmap(bitmap: Bitmap): String?
    fun logBitmap(width: Int, height: Int, config: Bitmap.Config): String?
    fun getSize(bitmap: Bitmap): Int
}