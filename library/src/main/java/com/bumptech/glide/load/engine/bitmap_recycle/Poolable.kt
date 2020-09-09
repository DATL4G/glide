package com.bumptech.glide.load.engine.bitmap_recycle

internal interface Poolable {
    fun offer()
}