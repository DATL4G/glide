package com.bumptech.glide.load.data.mediastore

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.bumptech.glide.load.ImageHeaderParser
import com.bumptech.glide.load.ImageHeaderParserUtils
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

internal class ThumbnailStreamOpener(
        private val parsers: List<ImageHeaderParser?>?,
        private val service: FileService,
        private val query: ThumbnailQuery,
        private val byteArrayPool: ArrayPool?,
        private val contentResolver: ContentResolver) {
    constructor(
            parsers: List<ImageHeaderParser?>?,
            query: ThumbnailQuery,
            byteArrayPool: ArrayPool?,
            contentResolver: ContentResolver) : this(parsers, DEFAULT_SERVICE, query, byteArrayPool, contentResolver) {
    }

    fun getOrientation(uri: Uri): Int {
        var `is`: InputStream? = null
        try {
            `is` = contentResolver.openInputStream(uri)
            return ImageHeaderParserUtils.getOrientation(parsers!!, `is`, byteArrayPool!!)
            // PMD.AvoidCatchingNPE framework method openInputStream can throw NPEs.
        } catch (e: IOException) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to open uri: $uri", e)
            }
        } catch (e: NullPointerException) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to open uri: $uri", e)
            }
        } finally {
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    // Ignored.
                }
            }
        }
        return ImageHeaderParser.UNKNOWN_ORIENTATION
    }

    @Throws(FileNotFoundException::class)
    fun open(uri: Uri): InputStream? {
        val path = getPath(uri)
        if (path.isNullOrEmpty()) {
            return null
        }

        val file = service[path]
        if (!isValid(file)) {
            return null
        }
        val thumbnailUri = Uri.fromFile(file)
        return try {
            contentResolver.openInputStream(thumbnailUri)
            // PMD.AvoidCatchingNPE framework method openInputStream can throw NPEs.
        } catch (e: NullPointerException) {
            throw (FileNotFoundException("NPE opening uri: $uri -> $thumbnailUri").initCause(e) as FileNotFoundException)
        }
    }

    private fun getPath(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = query.query(uri)
            if (cursor != null && cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        } catch (e: SecurityException) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to query for thumbnail for Uri: $uri", e)
            }
            null
        } finally {
            cursor?.close()
        }
    }

    private fun isValid(file: File): Boolean {
        return service.exists(file) && 0 < service.length(file)
    }

    companion object {
        private const val TAG = "ThumbStreamOpener"
        private val DEFAULT_SERVICE = FileService()
    }
}