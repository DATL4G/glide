package com.bumptech.glide.module

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * Parses [com.bumptech.glide.module.GlideModule] references out of the AndroidManifest file.
 */
// Used only in javadoc.
@Deprecated("")
class ManifestParser(private val context: Context) {
    fun parse(): List<GlideModule> {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Loading Glide modules")
        }
        val modules: MutableList<GlideModule> = ArrayList()
        try {
            val appInfo = context
                    .packageManager
                    .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            if (appInfo.metaData == null) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Got null app info metadata")
                }
                return modules
            }
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Got app info metadata: " + appInfo.metaData)
            }
            for (key in appInfo.metaData.keySet()) {
                if (GLIDE_MODULE_VALUE == appInfo.metaData[key]) {
                    modules.add(parseModule(key))
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Loaded Glide module: $key")
                    }
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException("Unable to find metadata to parse GlideModules", e)
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Finished loading Glide modules")
        }
        return modules
    }

    companion object {
        private const val TAG = "ManifestParser"
        private const val GLIDE_MODULE_VALUE = "GlideModule"
        private fun parseModule(className: String): GlideModule {
            val clazz: Class<*>
            clazz = try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                throw IllegalArgumentException("Unable to find GlideModule implementation", e)
            }
            var module: Any? = null
            try {
                module = clazz.getDeclaredConstructor().newInstance()
                // These can't be combined until API minimum is 19.
            } catch (e: InstantiationException) {
                throwInstantiateGlideModuleException(clazz, e)
            } catch (e: IllegalAccessException) {
                throwInstantiateGlideModuleException(clazz, e)
            } catch (e: NoSuchMethodException) {
                throwInstantiateGlideModuleException(clazz, e)
            } catch (e: InvocationTargetException) {
                throwInstantiateGlideModuleException(clazz, e)
            }
            if (module !is GlideModule) {
                throw RuntimeException("Expected instanceof GlideModule, but found: $module")
            }
            return module
        }

        private fun throwInstantiateGlideModuleException(clazz: Class<*>, e: Exception) {
            throw RuntimeException("Unable to instantiate GlideModule implementation for $clazz", e)
        }
    }
}