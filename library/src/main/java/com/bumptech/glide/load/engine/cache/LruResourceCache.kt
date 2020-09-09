package com.bumptech.glide.load.engine.cache

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.cache.MemoryCache.ResourceRemovedListener
import com.bumptech.glide.util.LruCache

/** An LRU in memory cache for [com.bumptech.glide.load.engine.Resource]s.  */
class LruResourceCache
/**
 * Constructor for LruResourceCache.
 *
 * @param size The maximum size in bytes the in memory cache can use.
 */
(private val size: Long) : LruCache<Key?, Resource<*>?>(size), MemoryCache {
    private var listener: ResourceRemovedListener? = null

    override fun put(key: Key, resource: Resource<*>?): Resource<*>? = putKey(key, resource)

    override fun remove(key: Key): Resource<*>? = removeKey(key)

    override fun setResourceRemovedListener(listener: ResourceRemovedListener) {
        this.listener = listener
    }

    public override fun onItemEvicted(key: Key?, item: Resource<*>?) {
        item?.let {
            listener?.onResourceRemoved(it)
        }
    }

    override fun getSize(item: Resource<*>?): Int {
        return item?.size ?: super.getSize(null)
    }

    @SuppressLint("InlinedApi")
    override fun trimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            // Entering list of cached background apps
            // Evict our entire bitmap cache
            clearMemory()
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
                || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            // The app's UI is no longer visible, or app is in the foreground but system is running
            // critically low on memory
            // Evict oldest half of our bitmap cache
            trimToSize(maxSize / 2)
        }
    }
}