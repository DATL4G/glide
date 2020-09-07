package com.bumptech.glide.signature

import com.bumptech.glide.load.Key
import java.security.MessageDigest

/** An empty key that is always equal to all other empty keys.  */
class EmptySignature private constructor() : Key {
    override fun toString(): String {
        return "EmptySignature"
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        // Do nothing.
    }

    companion object {
        private val EMPTY_KEY = EmptySignature()
        @JvmStatic
        fun obtain(): EmptySignature {
            return EMPTY_KEY
        }
    }
}