package com.bumptech.glide.load.engine.cache

import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.cache.MemoryCache.ResourceRemovedListener

/** A simple class that ignores all puts and returns null for all gets.  */
class MemoryCacheAdapter : MemoryCache {
    private var listener: ResourceRemovedListener? = null
    override val currentSize: Long
        get() = 0
    override val maxSize: Long
        get() = 0

    override fun setSizeMultiplier(multiplier: Float) {
        // Do nothing.
    }

    override fun remove(key: Key): Resource<*>? {
        return null
    }

    override fun put(key: Key, resource: Resource<*>?): Resource<*>? {
        if (resource != null) {
            listener?.onResourceRemoved(resource)
        }
        return null
    }

    override fun setResourceRemovedListener(listener: ResourceRemovedListener) {
        this.listener = listener
    }

    override fun clearMemory() {
        // Do nothing.
    }

    override fun trimMemory(level: Int) {
        // Do nothing.
    }
}