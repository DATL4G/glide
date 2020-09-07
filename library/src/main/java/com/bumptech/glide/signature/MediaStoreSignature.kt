package com.bumptech.glide.signature

import com.bumptech.glide.load.Key
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * A unique signature based on metadata data from the media store that detects common changes to
 * media store files like edits, rotations, and temporary file replacement.
 */
class MediaStoreSignature(mimeType: String?, private val dateModified: Long, private val orientation: Int) : Key {
    private val mimeType: String = mimeType ?: ""

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as MediaStoreSignature
        if (dateModified != that.dateModified) {
            return false
        }
        if (orientation != that.orientation) {
            return false
        }
        return mimeType == that.mimeType
    }

    override fun hashCode(): Int {
        var result = mimeType.hashCode()
        result = 31 * result + (dateModified xor (dateModified ushr 32)).toInt()
        result = 31 * result + orientation
        return result
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        val data = ByteBuffer.allocate(12).putLong(dateModified).putInt(orientation).array()
        messageDigest.update(data)
        messageDigest.update(mimeType.toByteArray(Key.CHARSET))
    }

}