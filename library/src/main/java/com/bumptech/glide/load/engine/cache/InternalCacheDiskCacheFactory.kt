package com.bumptech.glide.load.engine.cache

import android.content.Context
import java.io.File

/**
 * Creates an [com.bumptech.glide.disklrucache.DiskLruCache] based disk cache in the internal
 * disk cache directory.
 */
// Public API.
class InternalCacheDiskCacheFactory @JvmOverloads constructor(
        context: Context, diskCacheName: String? =
                DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, diskCacheSize: Long =
                DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE.toLong()) : DiskLruCacheFactory(
        object : CacheDirectoryGetter {
            override val cacheDirectory: File?
                get() {
                    val cacheDirectory = context.cacheDir ?: return null
                    return if (diskCacheName != null) {
                        File(cacheDirectory, diskCacheName)
                    } else cacheDirectory
                }
        },
        diskCacheSize) {
    constructor(context: Context, diskCacheSize: Long) : this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, diskCacheSize) {}
}