package com.bumptech.glide.load.engine.prefill

import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import com.bumptech.glide.util.Preconditions

/**
 * A container for a put of options used to pre-fill a [ ] with [Bitmaps][Bitmap] of a single
 * size and configuration.
 */
class PreFillType internal constructor(width: Int, height: Int, config: Bitmap.Config?, weight: Int) {
    /** Returns the width in pixels of the [Bitmaps][android.graphics.Bitmap].  */
    val width: Int
    /** Returns the height in pixels of the [Bitmaps][android.graphics.Bitmap].  */
    val height: Int

    /**
     * Returns the [android.graphics.Bitmap.Config] of the [ Bitmaps][android.graphics.Bitmap].
     */
    val config: Bitmap.Config
    /** Returns the weight of the [Bitmaps][android.graphics.Bitmap] of this type.  */
    val weight: Int

    /**
     * Constructor for a single type of [android.graphics.Bitmap].
     *
     * @param width The width in pixels of the [Bitmaps][android.graphics.Bitmap] to pre-fill.
     * @param height The height in pixels of the [Bitmaps][android.graphics.Bitmap] to pre-fill.
     * @param config The [android.graphics.Bitmap.Config] of the [     Bitmaps][android.graphics.Bitmap] to pre-fill.
     * @param weight An integer indicating how to balance pre-filling this size and configuration of
     * [android.graphics.Bitmap] against any other sizes/configurations that may be being
     * pre-filled.
     */
    init {
        this.config = Preconditions.checkNotNull(config, "Config must not be null")
        this.width = width
        this.height = height
        this.weight = weight
    }

    override fun equals(other: Any?): Boolean {
        if (other is PreFillType) {
            return height == other.height && width == other.width && weight == other.weight && config == other.config
        }
        return false
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + config.hashCode()
        result = 31 * result + weight
        return result
    }

    override fun toString(): String {
        return ("PreFillSize{"
                + "width="
                + width
                + ", height="
                + height
                + ", config="
                + config
                + ", weight="
                + weight
                + '}')
    }

    /** Builder for [PreFillType].  */
    class Builder(width: Int, height: Int) {
        private val width: Int
        private val height: Int
        /** Returns the current [android.graphics.Bitmap.Config].  */
        var config: Bitmap.Config? = null
            private set
        private var weight = 1

        /**
         * Constructor for a builder that uses the given size as the width and height of the Bitmaps to
         * prefill.
         *
         * @param size The width and height in pixels of the Bitmaps to prefill.
         */
        constructor(size: Int) : this(size, size) {}

        /**
         * Sets the [android.graphics.Bitmap.Config] for the Bitmaps to pre-fill.
         *
         * @param config The config to use, or null to use Glide's default.
         * @return This builder.
         */
        fun setConfig(config: Bitmap.Config?): Builder {
            this.config = config
            return this
        }

        /**
         * Sets the weight to use to balance how many Bitmaps of this type are prefilled relative to the
         * other requested types.
         *
         * @param weight An integer indicating how to balance pre-filling this size and configuration of
         * [android.graphics.Bitmap] against any other sizes/configurations that may be being
         * pre-filled.
         * @return This builder.
         */
        fun setWeight(weight: Int): Builder {
            require(weight > 0) { "Weight must be > 0" }
            this.weight = weight
            return this
        }

        /** Returns a new [PreFillType].  */
        fun build(): PreFillType {
            return PreFillType(width, height, config, weight)
        }

        /**
         * Constructor for a builder that uses the given dimensions as the dimensions of the Bitmaps to
         * prefill.
         *
         * @param width The width in pixels of the Bitmaps to prefill.
         * @param height The height in pixels of the Bitmaps to prefill.
         */
        init {
            require(width > 0) { "Width must be > 0" }
            require(height > 0) { "Height must be > 0" }
            this.width = width
            this.height = height
        }
    }

    companion object {
        @JvmField
        @VisibleForTesting
        val DEFAULT_CONFIG = Bitmap.Config.RGB_565
    }
}