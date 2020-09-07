package com.bumptech.glide.load.data

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource

/**
 * Lazily retrieves data that can be used to load a resource.
 *
 *
 * A new instance is created per resource load by [ ]. [.loadData] may or may not be called for any given
 * load depending on whether or not the corresponding resource is cached. Cancel also may or may not
 * be called. If [.loadData]} is called, then so [.cleanup]
 * will be called.
 *
 * @param <T> The type of data to be loaded (InputStream, byte[], File etc).
</T> */
interface DataFetcher<T> {
    /**
     * Callback that must be called when data has been loaded and is available, or when the load
     * fails.
     *
     * @param <T> The type of data that will be loaded.
    </T> */
    interface DataCallback<T> {
        /**
         * Called with the loaded data if the load succeeded, or with `null` if the load failed.
         */
        fun onDataReady(data: T?)

        /**
         * Called when the load fails.
         *
         * @param e a non-null [Exception] indicating why the load failed.
         */
        fun onLoadFailed(e: Exception)
    }

    /**
     * Fetch data from which a resource can be decoded.
     *
     *
     * This will always be called on background thread so it is safe to perform long running tasks
     * here. Any third party libraries called must be thread safe (or move the work to another thread)
     * since this method will be called from a thread in a [ ] that may have more than one background thread. You
     * **MUST** use the [DataCallback] once the request is complete.
     *
     *
     * You are free to move the fetch work to another thread and call the callback from there.
     *
     *
     * This method will only be called when the corresponding resource is not in the cache.
     *
     *
     * Note - this method will be run on a background thread so blocking I/O is safe.
     *
     * @param priority The priority with which the request should be completed.
     * @param callback The callback to use when the request is complete
     * @see .cleanup
     */
    fun loadData(priority: Priority, callback: DataCallback<in T>)

    /**
     * Cleanup or recycle any resources used by this data fetcher. This method will be called in a
     * finally block after the data provided by [.loadData] has been decoded by the [ ].
     *
     *
     * Note - this method will be run on a background thread so blocking I/O is safe.
     */
    fun cleanup()

    /**
     * A method that will be called when a load is no longer relevant and has been cancelled. This
     * method does not need to guarantee that any in process loads do not finish. It also may be
     * called before a load starts or after it finishes.
     *
     *
     * The best way to use this method is to cancel any loads that have not yet started, but allow
     * those that are in process to finish since its we typically will want to display the same
     * resource in a different view in the near future.
     *
     *
     * Note - this method will be run on the main thread so it should not perform blocking
     * operations and should finish quickly.
     */
    fun cancel()

    /** Returns the class of the data this fetcher will attempt to obtain.  */
    val dataClass: Class<T>

    /** Returns the [com.bumptech.glide.load.DataSource] this fetcher will return data from.  */
    val dataSource: DataSource
}