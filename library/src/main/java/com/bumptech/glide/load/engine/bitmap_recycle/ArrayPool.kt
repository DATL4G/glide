package com.bumptech.glide.load.engine.bitmap_recycle

/** Interface for an array pool that pools arrays of different types.  */
interface ArrayPool {
    /**
     * Optionally adds the given array of the given type to the pool.
     *
     *
     * Arrays may be ignored, for example if the array is larger than the maximum size of the pool.
     *
     */
    @Deprecated("Use {@link #put(Object)}")
    fun <T> put(array: T, arrayClass: Class<T>?)

    /**
     * Optionally adds the given array of the given type to the pool.
     *
     *
     * Arrays may be ignored, for example if the array is larger than the maximum size of the pool.
     */
    fun <T> put(array: T)

    /**
     * Returns a non-null array of the given type with a length >= to the given size.
     *
     *
     * If an array of the given size isn't in the pool, a new one will be allocated.
     *
     *
     * This class makes no guarantees about the contents of the returned array.
     *
     * @see .getExact
     */
    operator fun <T> get(size: Int, arrayClass: Class<T>?): T

    /**
     * Returns a non-null array of the given type with a length exactly equal to the given size.
     *
     *
     * If an array of the given size isn't in the pool, a new one will be allocated.
     *
     *
     * This class makes no guarantees about the contents of the returned array.
     *
     * @see .get
     */
    fun <T> getExact(size: Int, arrayClass: Class<T>?): T

    /** Clears all arrays from the pool.  */
    fun clearMemory()

    /**
     * Trims the size to the appropriate level.
     *
     * @param level A trim specified in [android.content.ComponentCallbacks2].
     */
    fun trimMemory(level: Int)

    companion object {
        /**
         * A standard size to use to increase hit rates when the required size isn't defined. Currently
         * 64KB.
         */
        const val STANDARD_BUFFER_SIZE_BYTES = 64 * 1024
    }
}