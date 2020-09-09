package com.bumptech.glide.load.engine.cache

import com.bumptech.glide.load.Key
import java.io.File

/** A simple class that returns null for all gets and ignores all writes.  */
class DiskCacheAdapter : DiskCache {
    override fun get(key: Key): File? {
        // no op, default for overriders
        return null
    }

    override fun put(key: Key, writer: DiskCache.Writer?) {
        // no op, default for overriders
    }

    override fun delete(key: Key) {
        // no op, default for overriders
    }

    override fun clear() {
        // no op, default for overriders
    }

    /** Default factory for [DiskCacheAdapter].  */
    class Factory : DiskCache.Factory {
        override fun build(): DiskCache? {
            return DiskCacheAdapter()
        }
    }
}