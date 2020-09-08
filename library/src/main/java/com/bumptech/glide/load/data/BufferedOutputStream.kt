package com.bumptech.glide.load.data

import androidx.annotation.VisibleForTesting
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
import java.io.IOException
import java.io.OutputStream
import kotlin.math.min

/**
 * An [OutputStream] implementation that recycles and re-uses `byte[]`s using the
 * provided [ArrayPool].
 */
class BufferedOutputStream @VisibleForTesting internal constructor(private val out: OutputStream, private val arrayPool: ArrayPool, bufferSize: Int) : OutputStream() {
    private var buffer: ByteArray?
    private var index = 0

    constructor(out: OutputStream, arrayPool: ArrayPool) : this(out, arrayPool, ArrayPool.STANDARD_BUFFER_SIZE_BYTES)

    @Throws(IOException::class)
    override fun write(b: Int) {
        buffer?.set(index++, b.toByte())
        maybeFlushBuffer()
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, initialOffset: Int, length: Int) {
        var writtenSoFar = 0
        do {
            val remainingToWrite = length - writtenSoFar
            val currentOffset = initialOffset + writtenSoFar
            // If we still need to write at least the buffer size worth of bytes, we might as well do so
            // directly and avoid the overhead of copying to the buffer first.
            if (index == 0 && (remainingToWrite >= buffer?.size ?: 0)) {
                out.write(b, currentOffset, remainingToWrite)
                return
            }

            val remainingSpaceInBuffer = buffer?.size?.minus(index) ?: 0
            val totalBytesToWriteToBuffer = min(remainingToWrite, remainingSpaceInBuffer)
            buffer?.let { System.arraycopy(b, currentOffset, it, index, totalBytesToWriteToBuffer) }
            index += totalBytesToWriteToBuffer
            writtenSoFar += totalBytesToWriteToBuffer
            maybeFlushBuffer()
        } while (writtenSoFar < length)
    }

    @Throws(IOException::class)
    override fun flush() {
        flushBuffer()
        out.flush()
    }

    @Throws(IOException::class)
    private fun flushBuffer() {
        if (index > 0) {
            out.write(buffer, 0, index)
            index = 0
        }
    }

    @Throws(IOException::class)
    private fun maybeFlushBuffer() {
        if (index == buffer!!.size) {
            flushBuffer()
        }
    }

    @Throws(IOException::class)
    override fun close() {
        out.use { flush() }
        release()
    }

    private fun release() {
        if (buffer != null) {
            arrayPool.put(buffer)
            buffer = null
        }
    }

    init {
        buffer = arrayPool.get(bufferSize, ByteArray::class.java)
    }
}