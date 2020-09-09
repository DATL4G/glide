package com.bumptech.glide.load.engine.cache

import android.content.Context
import java.io.File

/**
 * Creates an [com.bumptech.glide.disklrucache.DiskLruCache] based disk cache in the external
 * disk cache directory, which falls back to the internal disk cache if no external storage is
 * available. If ever fell back to the internal disk cache, will use that one from that moment on.
 *
 *
 * **Images can be read by everyone when using external disk cache.**
 */
// Public API.
class ExternalPreferredCacheDiskCacheFactory @JvmOverloads constructor(
        context: Context, diskCacheName: String? =
                DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, diskCacheSize: Long =
                DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE.toLong()) : DiskLruCacheFactory(
        object : CacheDirectoryGetter {
            private val internalCacheDirectory: File?
                get() {
                    val cacheDirectory = context.cacheDir ?: return null
                    return if (diskCacheName != null) {
                        File(cacheDirectory, diskCacheName)
                    } else cacheDirectory
                }
            // Already used internal cache, so keep using that one,
            // thus avoiding using both external and internal with transient errors.

            // Shared storage is not available.
            override val cacheDirectory: File?
                get() {
                    val internalCacheDirectory = internalCacheDirectory

                    // Already used internal cache, so keep using that one,
                    // thus avoiding using both external and internal with transient errors.
                    if (null != internalCacheDirectory && internalCacheDirectory.exists()) {
                        return internalCacheDirectory
                    }
                    val cacheDirectory = context.externalCacheDir

                    // Shared storage is not available.
                    if (cacheDirectory == null || !cacheDirectory.canWrite()) {
                        return internalCacheDirectory
                    }
                    return if (diskCacheName != null) {
                        File(cacheDirectory, diskCacheName)
                    } else cacheDirectory
                }
        },
        diskCacheSize) {
    constructor(context: Context, diskCacheSize: Long) : this(context, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR, diskCacheSize) {}
}