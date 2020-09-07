package com.bumptech.glide.signature

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.bumptech.glide.load.Key
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * A utility class for obtaining a [com.bumptech.glide.load.Key] signature containing the
 * application version name using [android.content.pm.PackageInfo.versionCode].
 */
object ApplicationVersionSignature {
    private const val TAG = "AppVersionSignature"
    private val PACKAGE_NAME_TO_KEY: ConcurrentMap<String, Key> = ConcurrentHashMap()

    /**
     * Returns the signature [com.bumptech.glide.load.Key] for version code of the Application
     * of the given Context.
     */
    @JvmStatic
    fun obtain(context: Context): Key {
        val packageName = context.packageName
        var result = PACKAGE_NAME_TO_KEY[packageName]
        if (result == null) {
            val toAdd = obtainVersionSignature(context)
            result = PACKAGE_NAME_TO_KEY.putIfAbsent(packageName, toAdd)
            // There wasn't a previous mapping, so toAdd is now the Key.
            if (result == null) {
                result = toAdd
            }
        }
        return result
    }

    @JvmStatic
    @VisibleForTesting
    fun reset() {
        PACKAGE_NAME_TO_KEY.clear()
    }

    private fun obtainVersionSignature(context: Context): Key {
        val packageInfo = getPackageInfo(context)
        val versionCode = getVersionCode(packageInfo)
        return ObjectKey(versionCode)
    }

    private fun getVersionCode(packageInfo: PackageInfo?): String {
        return packageInfo?.versionCode?.toString() ?: UUID.randomUUID().toString()
    }

    private fun getPackageInfo(context: Context): PackageInfo? {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Cannot resolve info for" + context.packageName, e)
            null
        }
    }
}