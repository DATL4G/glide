package com.bumptech.glide.load.data

import android.content.res.AssetManager
import android.util.Log
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import java.io.IOException

/**
 * An abstract class for obtaining data for an asset path using an [ ].
 *
 * @param <T> The type of data obtained from the asset path (InputStream, FileDescriptor etc).
</T> */
abstract class AssetPathFetcher<T>     // Public API.
(private val assetManager: AssetManager, private val assetPath: String) : DataFetcher<T> {
    private var data: T? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in T>) {
        data = try {
            loadResource(assetManager, assetPath)
        } catch (e: IOException) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to load data from asset manager", e)
            }
            callback.onLoadFailed(e)
            return
        }
        callback.onDataReady(data)
    }

    override fun cleanup() {
        if (data == null) {
            return
        }
        try {
            data?.let { close(it) }
        } catch (e: IOException) {
            // Ignored.
        }
    }

    override fun cancel() {
        // Do nothing.
    }

    override val dataSource: DataSource
        get() = DataSource.LOCAL

    /**
     * Opens the given asset path with the given [android.content.res.AssetManager] and returns
     * the concrete data type returned by the AssetManager.
     *
     * @param assetManager An AssetManager to use to open the given path.
     * @param path A string path pointing to a resource in assets to open.
     */
    @Throws(IOException::class)
    protected abstract fun loadResource(assetManager: AssetManager, path: String): T

    /**
     * Closes the concrete data type if necessary.
     *
     * @param data The data to close.
     */
    @Throws(IOException::class)
    protected abstract fun close(data: T)

    companion object {
        private const val TAG = "AssetPathFetcher"
    }
}