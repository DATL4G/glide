package com.bumptech.glide.module

import android.content.Context
import com.bumptech.glide.GlideBuilder

/**
 * Defines a set of dependencies and options to use when initializing Glide within an application.
 *
 *
 * There can be at most one [AppGlideModule] in an application. Only Applications can
 * include a [AppGlideModule]. Libraries must use [LibraryGlideModule].
 *
 *
 * Classes that extend [AppGlideModule] must be annotated with [ ] to be processed correctly.
 *
 *
 * Classes that extend [AppGlideModule] can optionally be annotated with [ ] to optionally exclude one or more [ ] and/or [GlideModule] classes.
 *
 *
 * Once an application has migrated itself and all libraries it depends on to use Glide's
 * annotation processor, [AppGlideModule] implementations should override [ ][.isManifestParsingEnabled] and return `false`.
 */
// Used only in javadoc.
abstract class AppGlideModule : LibraryGlideModule(), AppliesOptions {
    /**
     * Returns `true` if Glide should check the AndroidManifest for [GlideModule]s.
     *
     *
     * Implementations should return `false` after they and their dependencies have migrated
     * to Glide's annotation processor.
     *
     *
     * Returns `true` by default.
     */
    open val isManifestParsingEnabled: Boolean
        get() = true

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Default empty impl.
    }
}