package com.bumptech.glide.load.data

import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import androidx.annotation.RequiresApi
import java.io.IOException

/**
 * Implementation for [ParcelFileDescriptor]s that rewinds file descriptors by seeking to 0.
 */
class ParcelFileDescriptorRewinder @RequiresApi(Build.VERSION_CODES.LOLLIPOP) constructor(parcelFileDescriptor: ParcelFileDescriptor) : DataRewinder<ParcelFileDescriptor?> {
    private val rewinder: InternalRewinder

    init {
        rewinder = InternalRewinder(parcelFileDescriptor)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Throws(IOException::class)
    override fun rewindAndGet(): ParcelFileDescriptor {
        return rewinder.rewind()
    }

    override fun cleanup() {
        // Do nothing.
    }

    /**
     * Factory for producing [ParcelFileDescriptorRewinder]s from [ParcelFileDescriptor]s.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    class Factory : DataRewinder.Factory<ParcelFileDescriptor?> {
        override fun build(data: ParcelFileDescriptor?): DataRewinder<ParcelFileDescriptor?> {
            return ParcelFileDescriptorRewinder(data!!)
        }

        @Suppress("UNCHECKED_CAST")
        override val dataClass: Class<ParcelFileDescriptor?>
            get() = ParcelFileDescriptor::class.java as Class<ParcelFileDescriptor?>
    }

    /**
     * Catching ErrnoException cannot be done in classes that are loaded on APIs < Lollipop. To make
     * sure that we do not do so, we catch inside this inner class instead of the outer class. The
     * only reason this class exists is to avoid VerifyError on older APIs.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private class InternalRewinder(private val parcelFileDescriptor: ParcelFileDescriptor) {
        @Throws(IOException::class)
        fun rewind(): ParcelFileDescriptor {
            try {
                Os.lseek(parcelFileDescriptor.fileDescriptor, 0, OsConstants.SEEK_SET)
            } catch (e: ErrnoException) {
                throw IOException(e)
            }
            return parcelFileDescriptor
        }
    }

    companion object {
        // Os.lseek() is only supported on API 21+.
        val isSupported: Boolean
            get() =// Os.lseek() is only supported on API 21+.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

}