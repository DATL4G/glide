package com.bumptech.glide.module

import android.content.Context
import com.bumptech.glide.GlideBuilder

/** An internal interface, to be removed when [GlideModule]s are removed.  */
@Deprecated("")
interface AppliesOptions {
    /**
     * Lazily apply options to a [com.bumptech.glide.GlideBuilder] immediately before the Glide
     * singleton is created.
     *
     *
     * This method will be called once and only once per implementation.
     *
     * @param context An Application [android.content.Context].
     * @param builder The [com.bumptech.glide.GlideBuilder] that will be used to create Glide.
     */
    fun applyOptions(context: Context, builder: GlideBuilder)
}