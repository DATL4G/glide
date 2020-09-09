package com.bumptech.glide.load.engine.bitmap_recycle

/** Adapter for handling primitive byte arrays.  */
class ByteArrayAdapter : ArrayAdapterInterface<ByteArray?> {
    override fun getArrayLength(array: ByteArray?): Int {
        return array?.size ?: 0
    }

    override fun newArray(length: Int): ByteArray {
        return ByteArray(length)
    }

    override val elementSizeInBytes: Int
        get() = 1

    override val tag: String?
        get() = "ByteArrayPool"

}