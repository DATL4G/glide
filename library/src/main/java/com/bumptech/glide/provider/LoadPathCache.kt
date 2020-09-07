package com.bumptech.glide.provider

import androidx.collection.ArrayMap
import com.bumptech.glide.load.engine.DecodePath
import com.bumptech.glide.load.engine.LoadPath
import com.bumptech.glide.load.resource.transcode.UnitTranscoder
import com.bumptech.glide.util.MultiClassKey
import java.util.concurrent.atomic.AtomicReference

/**
 * Maintains a cache of data, resource, and transcode classes to available [ ]s capable of decoding with the requested types.
 */
class LoadPathCache {
    private val cache = ArrayMap<MultiClassKey, LoadPath<*, *, *>>()
    private val keyRef = AtomicReference<MultiClassKey?>()

    /**
     * Returns `` true if the given [LoadPath] is the signal object returned from [ ][.get] that indicates that we've previously found that there are no
     * available paths to load the requested resources and `false` otherwise.
     */
    fun isEmptyLoadPath(path: LoadPath<*, *, *>?): Boolean {
        return NO_PATHS_SIGNAL == path
    }

    /**
     * May return [.NO_PATHS_SIGNAL] to indicate that we've previously found that there are 0
     * available load paths for the requested types. Callers must check using [ ][.isEmptyLoadPath] before using any load path returned by this method.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <Data, TResource, Transcode> get(
            dataClass: Class<Data>, resourceClass: Class<TResource>, transcodeClass: Class<Transcode>): LoadPath<Data, TResource, Transcode>? {
        val key = getKey(dataClass, resourceClass, transcodeClass)
        var result: LoadPath<*, *, *>?
        synchronized(cache) { result = cache[key] }
        keyRef.set(key)
        return result as LoadPath<Data, TResource, Transcode>?
    }

    fun put(
            dataClass: Class<*>?,
            resourceClass: Class<*>?,
            transcodeClass: Class<*>?,
            loadPath: LoadPath<*, *, *>?) {
        synchronized(cache) {
            cache.put(
                    MultiClassKey(dataClass!!, resourceClass!!, transcodeClass),
                    loadPath ?: NO_PATHS_SIGNAL)
        }
    }

    private fun getKey(
            dataClass: Class<*>, resourceClass: Class<*>, transcodeClass: Class<*>): MultiClassKey {
        var key = keyRef.getAndSet(null)
        if (key == null) {
            key = MultiClassKey()
        }
        key[dataClass, resourceClass] = transcodeClass
        return key
    }

    companion object {
        private val NO_PATHS_SIGNAL: LoadPath<*, *, *> = LoadPath(
                Any::class.java,
                Any::class.java,
                Any::class.java, listOf(
                DecodePath(
                        Any::class.java,
                        Any::class.java,
                        Any::class.java, emptyList(),
                        UnitTranscoder(),  /*listPool=*/
                        null)),  /*listPool=*/
                null)
    }
}