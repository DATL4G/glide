package com.bumptech.glide.load.engine.cache

import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.Resource

/** An interface for adding and removing resources from an in memory cache.  */
interface MemoryCache {
    /** An interface that will be called whenever a bitmap is removed from the cache.  */
    interface ResourceRemovedListener {
        fun onResourceRemoved(removed: Resource<*>)
    }

    /** Returns the sum of the sizes of all the contents of the cache in bytes.  */
    val currentSize: Long

    /** Returns the current maximum size in bytes of the cache.  */
    val maxSize: Long

    /**
     * Adjust the maximum size of the cache by multiplying the original size of the cache by the given
     * multiplier.
     *
     *
     * If the size multiplier causes the size of the cache to be decreased, items will be evicted
     * until the cache is smaller than the new size.
     *
     * @param multiplier A size multiplier >= 0.
     */
    fun setSizeMultiplier(multiplier: Float)

    /**
     * Removes the value for the given key and returns it if present or null otherwise.
     *
     * @param key The key.
     */
    fun remove(key: Key): Resource<*>?

    /**
     * Add bitmap to the cache with the given key.
     *
     * @param key The key to retrieve the bitmap.
     * @param resource The [com.bumptech.glide.load.engine.EngineResource] to store.
     * @return The old value of key (null if key is not in map).
     */
    fun put(key: Key, resource: Resource<*>?): Resource<*>?

    /**
     * Set the listener to be called when a bitmap is removed from the cache.
     *
     * @param listener The listener.
     */
    fun setResourceRemovedListener(listener: ResourceRemovedListener)

    /** Evict all items from the memory cache.  */
    fun clearMemory()

    /**
     * Trim the memory cache to the appropriate level. Typically called on the callback onTrimMemory.
     *
     * @param level This integer represents a trim level as specified in [     ].
     */
    fun trimMemory(level: Int)
}