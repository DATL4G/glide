package com.bumptech.glide.load.data

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.experimental.and
import kotlin.math.min

/**
 * Adds an exif segment with an orientation attribute to a wrapped [InputStream] containing
 * image data.
 *
 *
 * This class assumes that the wrapped stream contains an image format that can contain exif
 * information and performs no verification.
 */
class ExifOrientationStream(`in`: InputStream?, orientation: Int) : FilterInputStream(`in`) {
    private val orientation: Byte
    private var position = 0

    init {
        require(!(orientation < -1 || orientation > 8)) { "Cannot add invalid orientation: $orientation" }
        this.orientation = orientation.toByte()
    }

    override fun markSupported(): Boolean {
        return false
    }

    // No need for synchronized since all we do is throw.
    override fun mark(readLimit: Int) {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun read(): Int {
        val result: Int = when {
            position < SEGMENT_START_POSITION || position > ORIENTATION_POSITION -> {
                super.read()
            }
            position == ORIENTATION_POSITION -> {
                orientation.toInt()
            }
            else -> EXIF_SEGMENT[position - SEGMENT_START_POSITION].and(0xFF.toByte()).toInt()
        }
        if (result != -1) {
            position++
        }
        return result
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, byteOffset: Int, byteCount: Int): Int {
        val read: Int
        when {
            position > ORIENTATION_POSITION -> {
                read = super.read(buffer, byteOffset, byteCount)
            }
            position == ORIENTATION_POSITION -> {
                buffer[byteOffset] = orientation
                read = 1
            }
            position < SEGMENT_START_POSITION -> {
                read = super.read(buffer, byteOffset, SEGMENT_START_POSITION - position)
            }
            else -> {
                read = min(ORIENTATION_POSITION - position, byteCount)
                System.arraycopy(EXIF_SEGMENT, position - SEGMENT_START_POSITION, buffer, byteOffset, read)
            }
        }
        if (read > 0) {
            position += read
        }
        return read
    }

    @Throws(IOException::class)
    override fun skip(byteCount: Long): Long {
        val skipped = super.skip(byteCount)
        if (skipped > 0) {
            // See https://errorprone.info/bugpattern/NarrowingCompoundAssignment.
            position = (position + skipped).toInt()
        }
        return skipped
    }

    // No need for synchronized since all we do is throw.
    @Throws(IOException::class)
    override fun reset() {
        throw UnsupportedOperationException()
    }

    companion object {
        /** Allow two bytes for the file format.  */
        private const val SEGMENT_START_POSITION = 2
        private val EXIF_SEGMENT = byteArrayOf(
                /* segment start id. */
                0xFF.toByte(),
                /* segment type. */
                0xE1.toByte(),
                /* segmentLength. */
                0x00,
                0x1C.toByte(),
                /* exif identifier. */
                0x45,
                0x78,
                0x69,
                0x66,
                0x00,
                0x00,
                /* motorola byte order (big endian). */
                0x4D.toByte(),
                0x4D.toByte(),
                /* filler? */
                0x00,
                0x00,
                /* first id offset. */
                0x00,
                0x00,
                0x00,
                0x08,
                /* tagCount. */
                0x00,
                0x01,
                /* exif tag type. */
                0x01,
                0x12,
                /* 2 byte format. */
                0x00,
                0x02,
                /* component count. */
                0x00,
                0x00,
                0x00,
                0x01,
                /* 2 byte orientation value, the first byte of which is always 0. */
                0x00)
        private val SEGMENT_LENGTH = EXIF_SEGMENT.size
        private val ORIENTATION_POSITION = SEGMENT_LENGTH + SEGMENT_START_POSITION
    }
}