package com.bumptech.glide.provider

import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.util.Synthetic
import java.util.*

/**
 * Contains an ordered list of [ResourceDecoder]s capable of decoding arbitrary data types
 * into arbitrary resource types from highest priority decoders to lowest priority decoders.
 */
class ResourceDecoderRegistry {
    private val bucketPriorityList: MutableList<String> = ArrayList()
    private val decoders: MutableMap<String, MutableList<Entry<*, *>>> = HashMap()

    @Synchronized
    fun setBucketPriorityList(buckets: List<String>) {
        val previousBuckets: List<String> = ArrayList(bucketPriorityList)
        bucketPriorityList.clear()
        // new ArrayList(List) and ArrayList#addAll(List) are both broken on some verisons of Android,
        // see #3296
        bucketPriorityList.addAll(buckets)
        for (previousBucket in previousBuckets) {
            if (!buckets.contains(previousBucket)) {
                // Keep any buckets from the previous list that aren't included here, but but them at the
                // end.
                bucketPriorityList.add(previousBucket)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T, R> getDecoders(
            dataClass: Class<T>, resourceClass: Class<R>): List<ResourceDecoder<T, R>> {
        val result: MutableList<ResourceDecoder<T, R>> = ArrayList()
        for (bucket in bucketPriorityList) {
            val entries = decoders[bucket]
                    ?: continue
            for (entry in entries) {
                if (entry.handles(dataClass, resourceClass)) {
                    result.add(entry.decoder as ResourceDecoder<T, R>)
                }
            }
        }
        // TODO: cache result list.
        return result
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T, R> getResourceClasses(
            dataClass: Class<T>, resourceClass: Class<R>): List<Class<R>> {
        val result: MutableList<Class<R>> = ArrayList()
        for (bucket in bucketPriorityList) {
            val entries = decoders[bucket]
                    ?: continue
            for (entry in entries) {
                if (entry.handles(dataClass, resourceClass)
                        && !result.contains(entry.resourceClass as Class<R>)) {
                    result.add(entry.resourceClass)
                }
            }
        }
        return result
    }

    @Synchronized
    fun <T, R> append(
            bucket: String,
            decoder: ResourceDecoder<T, R>,
            dataClass: Class<T>,
            resourceClass: Class<R>) {
        getOrAddEntryList(bucket).add(Entry(dataClass, resourceClass, decoder))
    }

    @Synchronized
    fun <T, R> prepend(
            bucket: String,
            decoder: ResourceDecoder<T, R>,
            dataClass: Class<T>,
            resourceClass: Class<R>) {
        getOrAddEntryList(bucket).add(0, Entry(dataClass, resourceClass, decoder))
    }

    @Synchronized
    private fun getOrAddEntryList(bucket: String): MutableList<Entry<*, *>> {
        if (!bucketPriorityList.contains(bucket)) {
            // Add this unspecified bucket as a low priority bucket.
            bucketPriorityList.add(bucket)
        }
        var entries = decoders[bucket]
        if (entries == null) {
            entries = ArrayList()
            decoders[bucket] = entries
        }
        return entries
    }

    private class Entry<T, R>(
            private val dataClass: Class<T>,
            @Synthetic val resourceClass: Class<R>,
            @Synthetic val decoder: ResourceDecoder<T, R>) {
        fun handles(dataClass: Class<*>, resourceClass: Class<*>): Boolean {
            return (this.dataClass.isAssignableFrom(dataClass)
                    && resourceClass.isAssignableFrom(this.resourceClass))
        }
    }
}