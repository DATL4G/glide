package com.bumptech.glide.provider

import com.bumptech.glide.load.ResourceEncoder
import com.bumptech.glide.util.Synthetic
import java.util.*

/**
 * Contains an ordered list of [ResourceEncoder]s capable of encoding arbitrary resource
 * types.
 */
class ResourceEncoderRegistry {
    // TODO: this should probably be a put.
    private val encoders: MutableList<Entry<*>> = ArrayList()

    @Synchronized
    fun <Z> append(
            resourceClass: Class<Z>, encoder: ResourceEncoder<Z>) {
        encoders.add(Entry(resourceClass, encoder))
    }

    @Synchronized
    fun <Z> prepend(
            resourceClass: Class<Z>, encoder: ResourceEncoder<Z>) {
        encoders.add(0, Entry(resourceClass, encoder))
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    operator fun <Z> get(resourceClass: Class<Z>): ResourceEncoder<Z>? {
        var i = 0
        val size = encoders.size
        while (i < size) {
            val entry = encoders[i]
            if (entry.handles(resourceClass)) {
                return entry.encoder as ResourceEncoder<Z>
            }
            i++
        }
        // TODO: throw an exception here?
        return null
    }

    private class Entry<T>(private val resourceClass: Class<T>, @Synthetic val encoder: ResourceEncoder<T>) {
        @Synthetic
        fun handles(resourceClass: Class<*>): Boolean {
            return this.resourceClass.isAssignableFrom(resourceClass)
        }
    }
}