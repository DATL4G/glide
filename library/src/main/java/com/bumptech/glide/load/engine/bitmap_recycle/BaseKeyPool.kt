package com.bumptech.glide.load.engine.bitmap_recycle

import com.bumptech.glide.util.Util
import java.util.*

internal abstract class BaseKeyPool<T : Poolable?> {
    private val keyPool: Queue<T> = Util.createQueue(MAX_SIZE)
    fun get(): T? {
        var result = keyPool.poll()
        if (result == null) {
            result = create()
        }
        return result
    }

    fun offer(key: T) {
        if (keyPool.size < MAX_SIZE) {
            keyPool.offer(key)
        }
    }

    abstract fun create(): T

    companion object {
        private const val MAX_SIZE = 20
    }
}