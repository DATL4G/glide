package com.bumptech.glide.load.engine.cache

import com.bumptech.glide.load.Key
import com.bumptech.glide.util.LruCache
import com.bumptech.glide.util.Preconditions
import com.bumptech.glide.util.Synthetic
import com.bumptech.glide.util.Util
import com.bumptech.glide.util.pool.FactoryPools
import com.bumptech.glide.util.pool.StateVerifier
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * A class that generates and caches safe and unique string file names from [ ]s.
 */
// Public API.
class SafeKeyGenerator {
    private val loadIdToSafeHash = LruCache<Key, String>(1000)
    private val digestPool = FactoryPools.threadSafe(
            10,
            FactoryPools.Factory {
                try {
                    return@Factory PoolableDigestContainer(MessageDigest.getInstance("SHA-256"))
                } catch (e: NoSuchAlgorithmException) {
                    throw RuntimeException(e)
                }
            })

    fun getSafeKey(key: Key): String {
        var safeKey: String?
        synchronized(loadIdToSafeHash) { safeKey = loadIdToSafeHash[key] }
        if (safeKey == null) {
            safeKey = calculateHexStringDigest(key)
        }
        synchronized(loadIdToSafeHash) { loadIdToSafeHash.putKey(key, safeKey) }
        return safeKey!!
    }

    private fun calculateHexStringDigest(key: Key): String {
        val container = Preconditions.checkNotNull(digestPool.acquire())
        return try {
            key.updateDiskCacheKey(container.messageDigest)
            // calling digest() will automatically reset()
            Util.sha256BytesToHex(container.messageDigest.digest())
        } finally {
            digestPool.release(container)
        }
    }

    private class PoolableDigestContainer internal constructor(@Synthetic val messageDigest: MessageDigest) : FactoryPools.Poolable {
        private val stateVerifier = StateVerifier.newInstance()
        override fun getVerifier(): StateVerifier {
            return stateVerifier
        }
    }
}