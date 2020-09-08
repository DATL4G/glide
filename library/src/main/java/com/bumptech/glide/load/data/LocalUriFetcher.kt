package com.bumptech.glide.load.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import java.io.FileNotFoundException
import java.io.IOException

/**
 * A DataFetcher that uses an [android.content.ContentResolver] to load data from a [ ] pointing to a local resource.
 *
 * @param <T> The type of data that will obtained for the given uri (For example, [     ] or [android.os.ParcelFileDescriptor].
</T> */
abstract class LocalUriFetcher<T>
/**
 * Opens an input stream for a uri pointing to a local asset. Only certain uris are supported
 *
 * @param contentResolver Any [android.content.ContentResolver].
 * @param uri A Uri pointing to a local asset. This load will fail if the uri isn't openable by
 * [ContentResolver.openInputStream]
 * @see ContentResolver.openInputStream
 */
// Public API.
(private val contentResolver: ContentResolver, private val uri: Uri) : DataFetcher<T> {
    private var data: T? = null
    override fun loadData(
            priority: Priority, callback: DataFetcher.DataCallback<in T>) {
        data = try {
            loadResource(uri, contentResolver)
        } catch (e: FileNotFoundException) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to open Uri", e)
            }
            callback.onLoadFailed(e)
            return
        }
        callback.onDataReady(data)
    }

    override fun cleanup() {
        data?.let {
            try {
                close(it)
            } catch (e: IOException) {
                // Ignored.
            }
        }
    }

    override fun cancel() {
        // Do nothing.
    }

    override val dataSource: DataSource
        get() = DataSource.LOCAL

    /**
     * Returns a concrete data type from the given [android.net.Uri] using the given [ ].
     */
    @Throws(FileNotFoundException::class)
    protected abstract fun loadResource(uri: Uri, contentResolver: ContentResolver): T

    /**
     * Closes the concrete data type if necessary.
     *
     *
     * Note - We can't rely on the closeable interface because it was added after our min API
     * level. See issue #157.
     *
     * @param data The data to close.
     */
    @Throws(IOException::class)
    protected abstract fun close(data: T)

    companion object {
        private const val TAG = "LocalUriFetcher"
    }
}