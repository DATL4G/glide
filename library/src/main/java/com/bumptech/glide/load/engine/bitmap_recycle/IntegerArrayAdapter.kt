package com.bumptech.glide.load.engine.bitmap_recycle

/** Adapter for handling primitive int arrays.  */
class IntegerArrayAdapter : ArrayAdapterInterface<IntArray?> {
    override fun getArrayLength(array: IntArray?): Int {
        return array?.size ?: 0
    }

    override fun newArray(length: Int): IntArray {
        return IntArray(length)
    }

    override val elementSizeInBytes: Int
        get() = 4

    override val tag: String
        get() = "IntegerArrayPool"
}