package com.bumptech.glide.load.data

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.net.Uri
import java.io.FileNotFoundException
import java.io.IOException

/** Fetches an [AssetFileDescriptor] for a local [android.net.Uri].  */
class AssetFileDescriptorLocalUriFetcher(contentResolver: ContentResolver, uri: Uri) : LocalUriFetcher<AssetFileDescriptor?>(contentResolver, uri) {

    @Throws(FileNotFoundException::class)
    override fun loadResource(uri: Uri, contentResolver: ContentResolver): AssetFileDescriptor {
        return contentResolver.openAssetFileDescriptor(uri, "r")
                ?: throw FileNotFoundException("FileDescriptor is null for: $uri")
    }

    @Throws(IOException::class)
    override fun close(data: AssetFileDescriptor?) {
        data?.close()
    }

    @Suppress("UNCHECKED_CAST")
    override val dataClass: Class<AssetFileDescriptor?>
        get() = AssetFileDescriptor::class.java as Class<AssetFileDescriptor?>
}