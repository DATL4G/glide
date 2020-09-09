package com.bumptech.glide.load.engine.cache

import android.content.Context
import java.io.File

/**
 * Creates an [com.bumptech.glide.disklrucache.DiskLruCache] based disk cache in the external
 * disk cache directory.
 *
 *
 * **Images can be read by everyone when using external disk cache.**
 *
 */
// Public API.
@Deprecated("use {@link ExternalPreferredCacheDiskCacheFactory} instead.")
class ExternalCacheDiskCacheFactory @JvmOverloads constructor(
        context: Context, diskCacheName: String? =
                DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, diskCacheSize: Int =
                DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE) : DiskLruCacheFactory(
        object : CacheDirectoryGetter {
            override val cacheDirectory: File?
                get() {
                    val cacheDirectory = context.externalCacheDir ?: return null
                    return if (diskCacheName != null) {
                        File(cacheDirectory, diskCacheName)
                    } else cacheDirectory
                }
        },
        diskCacheSize.toLong()) {
    constructor(context: Context, diskCacheSize: Int) : this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, diskCacheSize) {}
}