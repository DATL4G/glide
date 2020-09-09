/*
 * Copyright (c) 2013. Bump Technologies Inc. All Rights Reserved.
 */
package com.bumptech.glide.load.engine.cache

import android.util.Log
import com.bumptech.glide.disklrucache.DiskLruCache
import com.bumptech.glide.load.Key
import java.io.File
import java.io.IOException

/**
 * The default DiskCache implementation. There must be no more than one active instance for a given
 * directory at a time.
 *
 * @see .get
 */
open class DiskLruCacheWrapper @Deprecated("Do not extend this class. ") protected constructor(private val directory: File?, private val maxSize: Long) : DiskCache {
    private val safeKeyGenerator: SafeKeyGenerator = SafeKeyGenerator()
    private val writeLocker = DiskCacheWriteLocker()
    private var diskLruCache: DiskLruCache? = null

    @get:Throws(IOException::class)
    @get:Synchronized
    private val diskCache: DiskLruCache?
        get() {
            if (diskLruCache == null) {
                diskLruCache = DiskLruCache.open(directory, APP_VERSION, VALUE_COUNT, maxSize)
            }
            return diskLruCache
        }

    override fun get(key: Key): File? {
        val safeKey = safeKeyGenerator.getSafeKey(key)
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Get: Obtained: $safeKey for for Key: $key")
        }
        var result: File? = null
        try {
            // It is possible that the there will be a put in between these two gets. If so that shouldn't
            // be a problem because we will always put the same value at the same key so our input streams
            // will still represent the same data.
            val value = diskCache!![safeKey]
            if (value != null) {
                result = value.getFile(0)
            }
        } catch (e: IOException) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unable to get from disk cache", e)
            }
        }
        return result
    }

    override fun put(key: Key, writer: DiskCache.Writer?) {
        // We want to make sure that puts block so that data is available when put completes. We may
        // actually not write any data if we find that data is written by the time we acquire the lock.
        val safeKey = safeKeyGenerator.getSafeKey(key)
        writeLocker.acquire(safeKey)
        try {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Put: Obtained: $safeKey for for Key: $key")
            }
            try {
                // We assume we only need to put once, so if data was written while we were trying to get
                // the lock, we can simply abort.
                val diskCache = diskCache
                val current = diskCache!![safeKey]
                if (current != null) {
                    return
                }
                val editor = diskCache.edit(safeKey)
                        ?: throw IllegalStateException("Had two simultaneous puts for: $safeKey")
                try {
                    val file = editor.getFile(0)
                    if (writer?.write(file) == true) {
                        editor.commit()
                    }
                } finally {
                    editor.abortUnlessCommitted()
                }
            } catch (e: IOException) {
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Unable to put to disk cache", e)
                }
            }
        } finally {
            writeLocker.release(safeKey)
        }
    }

    override fun delete(key: Key) {
        val safeKey = safeKeyGenerator.getSafeKey(key)
        try {
            diskCache?.remove(safeKey)
        } catch (e: IOException) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unable to delete from disk cache", e)
            }
        }
    }

    @Synchronized
    override fun clear() {
        try {
            diskCache?.delete()
        } catch (e: IOException) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unable to clear disk cache or disk cache cleared externally", e)
            }
        } finally {
            // Delete can close the cache but still throw. If we don't null out the disk cache here, every
            // subsequent request will try to act on a closed disk cache and fail. By nulling out the disk
            // cache we at least allow for attempts to open the cache in the future. See #2465.
            resetDiskCache()
        }
    }

    @Synchronized
    private fun resetDiskCache() {
        diskLruCache = null
    }

    companion object {
        private const val TAG = "DiskLruCacheWrapper"
        private const val APP_VERSION = 1
        private const val VALUE_COUNT = 1
        private var wrapper: DiskLruCacheWrapper? = null

        /**
         * Get a DiskCache in the given directory and size. If a disk cache has already been created with
         * a different directory and/or size, it will be returned instead and the new arguments will be
         * ignored.
         *
         * @param directory The directory for the disk cache
         * @param maxSize The max size for the disk cache
         * @return The new disk cache with the given arguments, or the current cache if one already exists
         */
        @Deprecated("Use {@link #create(File, long)} to create a new cache with the specified arguments.")
        @Synchronized
        operator fun get(directory: File?, maxSize: Long): DiskCache? {
            // TODO calling twice with different arguments makes it return the cache for the same
            // directory, it's public!
            if (wrapper == null) {
                wrapper = DiskLruCacheWrapper(directory, maxSize)
            }
            return wrapper
        }

        /**
         * Create a new DiskCache in the given directory with a specified max size.
         *
         * @param directory The directory for the disk cache
         * @param maxSize The max size for the disk cache
         * @return The new disk cache with the given arguments
         */
        @JvmStatic
        fun create(directory: File?, maxSize: Long): DiskCache {
            return DiskLruCacheWrapper(directory, maxSize)
        }
    }

}