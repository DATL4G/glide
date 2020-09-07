package com.bumptech.glide.load.data

import android.content.res.AssetManager
import android.os.ParcelFileDescriptor
import com.bumptech.glide.load.DataSource
import java.io.IOException

/** Fetches an [android.os.ParcelFileDescriptor] for an asset path.  */
class FileDescriptorAssetPathFetcher(assetManager: AssetManager?, assetPath: String?) : AssetPathFetcher<ParcelFileDescriptor?>(assetManager!!, assetPath!!) {
    @Throws(IOException::class)
    override fun loadResource(assetManager: AssetManager?, path: String?): ParcelFileDescriptor {
        return assetManager!!.openFd(path!!).parcelFileDescriptor
    }

    @Throws(IOException::class)
    override fun close(data: ParcelFileDescriptor?) {
        data?.close()
    }

    @Suppress("UNCHECKED_CAST")
    override val dataClass: Class<ParcelFileDescriptor?>
        get() = ParcelFileDescriptor::class.java as Class<ParcelFileDescriptor?>
}