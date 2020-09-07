package com.bumptech.glide.load.data.mediastore

import android.database.Cursor
import android.net.Uri

internal interface ThumbnailQuery {
    fun query(uri: Uri?): Cursor?
}