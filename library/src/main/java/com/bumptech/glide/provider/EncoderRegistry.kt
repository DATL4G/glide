package com.bumptech.glide.provider

import com.bumptech.glide.load.Encoder
import com.bumptech.glide.util.Synthetic
import java.util.*

/** Contains an ordered list of [Encoder]s capable of encoding arbitrary data types.  */
class EncoderRegistry {
    // TODO: This registry should probably contain a put, rather than a list.
    private val encoders: MutableList<Entry<*>> = ArrayList()

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T> getEncoder(dataClass: Class<T>): Encoder<T>? {
        for (entry in encoders) {
            if (entry.handles(dataClass)) {
                return entry.encoder as Encoder<T>
            }
        }
        return null
    }

    @Synchronized
    fun <T> append(dataClass: Class<T>, encoder: Encoder<T>) {
        encoders.add(Entry(dataClass, encoder))
    }

    @Synchronized
    fun <T> prepend(dataClass: Class<T>, encoder: Encoder<T>) {
        encoders.add(0, Entry(dataClass, encoder))
    }

    private class Entry<T>(private val dataClass: Class<T>, @Synthetic val encoder: Encoder<T>) {
        fun handles(dataClass: Class<*>): Boolean {
            return this.dataClass.isAssignableFrom(dataClass)
        }
    }
}