package com.bumptech.glide.load.data

import java.io.IOException

/**
 * Responsible for rewinding a stream like data types.
 *
 * @param <T> The stream like data type that can be rewound.
</T> */
interface DataRewinder<T> {
    /**
     * A factory interface for producing individual [ ]s.
     *
     * @param <T> The type of data that the [com.bumptech.glide.load.data.DataRewinder] will
     * wrap.
    </T> */
    interface Factory<T> {
        /** Returns a new [com.bumptech.glide.load.data.DataRewinder] wrapping the given data.  */
        fun build(data: T): DataRewinder<T>

        /**
         * Returns the class of data this factory can produce [ ]s for.
         */
        val dataClass: Class<T>
    }

    /**
     * Rewinds the wrapped data back to the beginning and returns the re-wound data (or a wrapper for
     * the re-wound data).
     *
     * @return An object pointing to the wrapped data.
     */
    @Throws(IOException::class)
    fun rewindAndGet(): T

    /**
     * Called when this rewinder is no longer needed and can be cleaned up.
     *
     *
     * The underlying data may still be in use and should not be closed or invalidated.
     */
    fun cleanup()
}