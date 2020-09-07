package com.bumptech.glide.signature

import android.content.Context
import android.content.res.Configuration
import com.bumptech.glide.load.Key
import com.bumptech.glide.util.Util
import java.nio.ByteBuffer
import java.security.MessageDigest

/** Includes information about the package as well as whether or not the device is in night mode.  */
class AndroidResourceSignature private constructor(private val nightMode: Int, private val applicationVersion: Key) : Key {

    override fun equals(other: Any?): Boolean {
        if (other is AndroidResourceSignature) {
            return nightMode == other.nightMode && applicationVersion == other.applicationVersion
        }
        return false
    }

    override fun hashCode(): Int {
        return Util.hashCode(applicationVersion, nightMode)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        applicationVersion.updateDiskCacheKey(messageDigest)
        val nightModeData = ByteBuffer.allocate(4).putInt(nightMode).array()
        messageDigest.update(nightModeData)
    }

    companion object {
        @JvmStatic
        fun obtain(context: Context): Key {
            val signature = ApplicationVersionSignature.obtain(context)
            val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return AndroidResourceSignature(nightMode, signature)
        }
    }
}