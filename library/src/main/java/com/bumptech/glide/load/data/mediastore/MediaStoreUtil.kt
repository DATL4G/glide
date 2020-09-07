package com.bumptech.glide.load.data.mediastore

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import com.bumptech.glide.request.target.Target

/** Utility classes for interacting with the media store.  */
object MediaStoreUtil {
    private const val MINI_THUMB_WIDTH = 512
    private const val MINI_THUMB_HEIGHT = 384
    @JvmStatic
    fun isMediaStoreUri(uri: Uri?): Boolean {
        return uri != null && ContentResolver.SCHEME_CONTENT == uri.scheme && MediaStore.AUTHORITY == uri.authority
    }

    private fun isVideoUri(uri: Uri): Boolean {
        return uri.pathSegments.contains("video")
    }

    @JvmStatic
    fun isMediaStoreVideoUri(uri: Uri): Boolean {
        return isMediaStoreUri(uri) && isVideoUri(uri)
    }

    @JvmStatic
    fun isMediaStoreImageUri(uri: Uri): Boolean {
        return isMediaStoreUri(uri) && !isVideoUri(uri)
    }

    @JvmStatic
    fun isThumbnailSize(width: Int, height: Int): Boolean {
        return width != Target.SIZE_ORIGINAL && height != Target.SIZE_ORIGINAL && width <= MINI_THUMB_WIDTH && height <= MINI_THUMB_HEIGHT
    }
}