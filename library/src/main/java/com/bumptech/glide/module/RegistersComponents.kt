package com.bumptech.glide.module

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry

/** An internal interface, to be removed when [GlideModule]s are removed.  */
// Used only in javadocs.
@Deprecated("")
interface RegistersComponents {

    /**
     * Lazily register components immediately after the Glide singleton is created but before any
     * requests can be started.
     *
     *
     * This method will be called once and only once per implementation.
     *
     * @param context An Application [android.content.Context].
     * @param glide The Glide singleton that is in the process of being initialized.
     * @param registry An [com.bumptech.glide.Registry] to use to register components.
     */
    fun registerComponents(context: Context, glide: Glide, registry: Registry)
}