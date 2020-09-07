package com.bumptech.glide.load.data.mediastore

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.data.ExifOrientationStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * A [DataFetcher] implementation for [InputStream]s that loads data from thumbnail
 * files obtained from the [MediaStore].
 */
class ThumbFetcher @VisibleForTesting internal constructor(private val mediaStoreImageUri: Uri, private val opener: ThumbnailStreamOpener) : DataFetcher<InputStream?> {
    private var inputStream: InputStream? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream?>) {
        inputStream = try {
            openThumbInputStream()
        } catch (e: FileNotFoundException) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to find thumbnail file", e)
            }
            callback.onLoadFailed(e)
            return
        }
        callback.onDataReady(inputStream)
    }

    @Throws(FileNotFoundException::class)
    private fun openThumbInputStream(): InputStream? {
        var result = opener.open(mediaStoreImageUri)
        var orientation = -1
        if (result != null) {
            orientation = opener.getOrientation(mediaStoreImageUri)
        }
        if (orientation != -1) {
            result = ExifOrientationStream(result, orientation)
        }
        return result
    }

    override fun cleanup() {
        if (inputStream != null) {
            try {
                inputStream!!.close()
            } catch (e: IOException) {
                // Ignored.
            }
        }
    }

    override fun cancel() {
        // Do nothing.
    }

    @Suppress("UNCHECKED_CAST")
    override val dataClass: Class<InputStream?>
        get() = InputStream::class.java as Class<InputStream?>

    override val dataSource: DataSource
        get() = DataSource.LOCAL

    internal class VideoThumbnailQuery(private val contentResolver: ContentResolver) : ThumbnailQuery {

        @SuppressLint("Recycle")
        override fun query(uri: Uri?): Cursor? {
            val videoId = uri?.lastPathSegment
            return contentResolver.query(
                    MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                    PATH_PROJECTION,
                    PATH_SELECTION, arrayOf(videoId),
                    null /*sortOrder*/)
        }

        companion object {
            private val PATH_PROJECTION = arrayOf(MediaStore.Video.Thumbnails.DATA)
            private const val PATH_SELECTION = (MediaStore.Video.Thumbnails.KIND
                    + " = "
                    + MediaStore.Video.Thumbnails.MINI_KIND
                    + " AND "
                    + MediaStore.Video.Thumbnails.VIDEO_ID
                    + " = ?")
        }
    }

    internal class ImageThumbnailQuery(private val contentResolver: ContentResolver) : ThumbnailQuery {

        @SuppressLint("Recycle")
        override fun query(uri: Uri?): Cursor? {
            val imageId = uri?.lastPathSegment
            return contentResolver.query(
                    MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                    PATH_PROJECTION,
                    PATH_SELECTION, arrayOf(imageId),
                    null /*sortOrder*/)
        }

        companion object {
            private val PATH_PROJECTION = arrayOf(
                    MediaStore.Images.Thumbnails.DATA)
            private const val PATH_SELECTION = (MediaStore.Images.Thumbnails.KIND
                    + " = "
                    + MediaStore.Images.Thumbnails.MINI_KIND
                    + " AND "
                    + MediaStore.Images.Thumbnails.IMAGE_ID
                    + " = ?")
        }
    }

    companion object {
        private const val TAG = "MediaStoreThumbFetcher"

        @JvmStatic
        fun buildImageFetcher(context: Context, uri: Uri): ThumbFetcher {
            return build(context, uri, ImageThumbnailQuery(context.contentResolver))
        }

        @JvmStatic
        fun buildVideoFetcher(context: Context, uri: Uri): ThumbFetcher {
            return build(context, uri, VideoThumbnailQuery(context.contentResolver))
        }

        private fun build(context: Context, uri: Uri, query: ThumbnailQuery): ThumbFetcher {
            val byteArrayPool = Glide.get(context).arrayPool
            val opener = ThumbnailStreamOpener(
                    Glide.get(context).registry.imageHeaderParsers,
                    query,
                    byteArrayPool,
                    context.contentResolver)
            return ThumbFetcher(uri, opener)
        }
    }

}