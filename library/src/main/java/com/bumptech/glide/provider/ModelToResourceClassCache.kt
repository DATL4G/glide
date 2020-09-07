package com.bumptech.glide.provider

import androidx.collection.ArrayMap
import com.bumptech.glide.util.MultiClassKey
import java.util.concurrent.atomic.AtomicReference

/**
 * Maintains a cache of Model + Resource class to a set of registered resource classes that are
 * subclasses of the resource class that can be decoded from the model class.
 */
class ModelToResourceClassCache {
    private val resourceClassKeyRef = AtomicReference<MultiClassKey>()
    private val registeredResourceClassCache: ArrayMap<MultiClassKey, List<Class<*>>> = ArrayMap()

    operator fun get(modelClass: Class<*>,
                     resourceClass: Class<*>,
                     transcodeClass: Class<*>): List<Class<*>>? {
        var key = resourceClassKeyRef.getAndSet(null)
        if (key == null) {
            key = MultiClassKey(modelClass, resourceClass, transcodeClass)
        } else {
            key.set(modelClass, resourceClass, transcodeClass)
        }
        val result: List<Class<*>>? = synchronized(registeredResourceClassCache) {
            registeredResourceClassCache[key]
        }
        resourceClassKeyRef.set(key)
        return result
    }

    fun put(modelClass: Class<*>,
            resourceClass: Class<*>,
            transcodeClass: Class<*>,
            resourceClasses: List<Class<*>>) = synchronized(registeredResourceClassCache) {
        registeredResourceClassCache.put(
                MultiClassKey(modelClass, resourceClass, transcodeClass), resourceClasses
        )
    }

    fun clear() = synchronized(registeredResourceClassCache) {
        registeredResourceClassCache.clear()
    }
}