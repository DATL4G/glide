package com.bumptech.glide.load.engine.cache

import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory.CacheDirectoryGetter
import java.io.File

/**
 * Creates an [com.bumptech.glide.disklrucache.DiskLruCache] based disk cache in the specified
 * disk cache directory.
 *
 *
 * If you need to make I/O access before returning the cache directory use the [ ][DiskLruCacheFactory.DiskLruCacheFactory] constructor variant.
 */
// Public API.
open class DiskLruCacheFactory
/**
 * When using this constructor [CacheDirectoryGetter.getCacheDirectory] will be called out
 * of UI thread, allowing to do I/O access without performance impacts.
 *
 * @param cacheDirectoryGetter Interface called out of UI thread to get the cache folder.
 * @param diskCacheSize Desired max bytes size for the LRU disk cache.
 */
// Public API.
(private val cacheDirectoryGetter: CacheDirectoryGetter, private val diskCacheSize: Long) : DiskCache.Factory {
    /** Interface called out of UI thread to get the cache folder.  */
    interface CacheDirectoryGetter {
        val cacheDirectory: File?
    }

    constructor(diskCacheFolder: String, diskCacheSize: Long) : this(
            object : CacheDirectoryGetter {
                override val cacheDirectory: File
                    get() = File(diskCacheFolder)
            },
            diskCacheSize) {
    }

    constructor(
            diskCacheFolder: String, diskCacheName: String, diskCacheSize: Long) : this(
            object : CacheDirectoryGetter {
                override val cacheDirectory: File
                    get() = File(diskCacheFolder, diskCacheName)
            },
            diskCacheSize) {
    }

    override fun build(): DiskCache? {
        val cacheDir = cacheDirectoryGetter.cacheDirectory ?: return null
        return if (!cacheDir.mkdirs() && (!cacheDir.exists() || !cacheDir.isDirectory)) {
            null
        } else DiskLruCacheWrapper.create(cacheDir, diskCacheSize)
    }
}