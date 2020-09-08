package com.bumptech.glide.load.data

import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
import com.bumptech.glide.load.resource.bitmap.RecyclableBufferedInputStream
import com.bumptech.glide.util.Synthetic
import java.io.IOException
import java.io.InputStream

/**
 * Implementation for [InputStream]s that rewinds streams by wrapping them in a buffered
 * stream.
 */
class InputStreamRewinder @Synthetic constructor(`is`: InputStream?, byteArrayPool: ArrayPool?) : DataRewinder<InputStream?> {
    private val bufferedStream: RecyclableBufferedInputStream = RecyclableBufferedInputStream(`is`!!, byteArrayPool!!)

    @Throws(IOException::class)
    override fun rewindAndGet(): InputStream {
        bufferedStream.reset()
        return bufferedStream
    }

    override fun cleanup() {
        bufferedStream.release()
    }

    fun fixMarkLimits() {
        bufferedStream.fixMarkLimit()
    }

    /**
     * Factory for producing [com.bumptech.glide.load.data.InputStreamRewinder]s from [ ]s.
     */
    class Factory(private val byteArrayPool: ArrayPool) : DataRewinder.Factory<InputStream?> {
        override fun build(data: InputStream?): DataRewinder<InputStream?> {
            return InputStreamRewinder(data, byteArrayPool)
        }

        @Suppress("UNCHECKED_CAST")
        override val dataClass: Class<InputStream?>
            get() = InputStream::class.java as Class<InputStream?>
    }

    companion object {
        // 5MB.
        private const val MARK_READ_LIMIT = 5 * 1024 * 1024
    }

    init {
        // We don't check is.markSupported() here because RecyclableBufferedInputStream allows resetting
        // after exceeding MARK_READ_LIMIT, which other InputStreams don't guarantee.
        bufferedStream.mark(MARK_READ_LIMIT)
    }
}