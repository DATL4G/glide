package com.bumptech.glide.load.data

import android.content.res.AssetManager
import java.io.IOException
import java.io.InputStream

/** Fetches an [java.io.InputStream] for an asset path.  */
class StreamAssetPathFetcher(assetManager: AssetManager, assetPath: String) : AssetPathFetcher<InputStream?>(assetManager, assetPath) {

    @Throws(IOException::class)
    override fun loadResource(assetManager: AssetManager, path: String): InputStream? {
        return assetManager.open(path)
    }

    @Throws(IOException::class)
    override fun close(data: InputStream?) {
        data?.close()
    }

    @Suppress("UNCHECKED_CAST")
    override val dataClass: Class<InputStream?>
        get() = InputStream::class.java as Class<InputStream?>
}