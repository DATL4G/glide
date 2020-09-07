package com.bumptech.glide.module

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry

/**
 * Registers a set of components to use when initializing Glide within an app when Glide's
 * annotation processor is used.
 *
 *
 * Any number of LibraryGlideModules can be contained within any library or application.
 *
 *
 * LibraryGlideModules are called in no defined order. If LibraryGlideModules within an
 * application conflict, [AppGlideModule]s can use the [ ] annotation to selectively remove one or more of the
 * conflicting modules.
 */
abstract class LibraryGlideModule : RegistersComponents {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Default empty impl.
    }
}