package com.bumptech.glide.load.data

import com.bumptech.glide.util.Preconditions
import java.util.*

/**
 * Stores a mapping of data class to [com.bumptech.glide.load.data.DataRewinder.Factory] and
 * allows registration of new types and factories.
 */
class DataRewinderRegistry {
    private val rewinders: MutableMap<Class<*>, DataRewinder.Factory<*>> = HashMap()

    @Synchronized
    fun register(factory: DataRewinder.Factory<*>) {
        rewinders[factory.dataClass] = factory
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T> build(data: T): DataRewinder<T> {
        Preconditions.checkNotNull(data)
        var result: DataRewinder.Factory<T>? = rewinders[data!!::class.java] as DataRewinder.Factory<T>?
        if (result == null) {
            for (registeredFactory in rewinders.values) {
                if (registeredFactory.dataClass.isAssignableFrom(data.javaClass)) {
                    result = registeredFactory as DataRewinder.Factory<T>
                    break
                }
            }
        }
        if (result == null) {
            result = DEFAULT_FACTORY as DataRewinder.Factory<T>
        }
        return result.build(data)
    }

    private class DefaultRewinder(private val data: Any) : DataRewinder<Any?> {
        override fun rewindAndGet(): Any {
            return data
        }

        override fun cleanup() {
            // Do nothing.
        }
    }

    companion object {
        private val DEFAULT_FACTORY: DataRewinder.Factory<*> = object : DataRewinder.Factory<Any?> {
            override fun build(data: Any?): DataRewinder<Any?> {
                return DefaultRewinder(data!!)
            }

            override val dataClass: Class<Any?>
                get() {
                    throw UnsupportedOperationException("Not implemented")
                }
        }
    }
}