package com.bumptech.glide.load.data

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileNotFoundException
import java.io.IOException

/** Fetches an [android.os.ParcelFileDescriptor] for a local [android.net.Uri].  */
class FileDescriptorLocalUriFetcher(contentResolver: ContentResolver, uri: Uri) : LocalUriFetcher<ParcelFileDescriptor?>(contentResolver, uri) {

    @Throws(FileNotFoundException::class)
    override fun loadResource(uri: Uri, contentResolver: ContentResolver): ParcelFileDescriptor {
        val assetFileDescriptor = contentResolver.openAssetFileDescriptor(uri, "r")
                ?: throw FileNotFoundException("FileDescriptor is null for: $uri")
        return assetFileDescriptor.parcelFileDescriptor
    }

    @Throws(IOException::class)
    override fun close(data: ParcelFileDescriptor?) {
        data?.close()
    }

    @Suppress("UNCHECKED_CAST")
    override val dataClass: Class<ParcelFileDescriptor?>
        get() = ParcelFileDescriptor::class.java as Class<ParcelFileDescriptor?>
}