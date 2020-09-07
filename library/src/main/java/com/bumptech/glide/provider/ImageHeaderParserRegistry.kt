package com.bumptech.glide.provider

import com.bumptech.glide.load.ImageHeaderParser
import java.util.*

/** Contains an unordered list of [ImageHeaderParser]s capable of parsing image headers.  */
class ImageHeaderParserRegistry {
    private val parsers: MutableList<ImageHeaderParser> = ArrayList()

    @Synchronized
    fun getParsers(): List<ImageHeaderParser> {
        return parsers
    }

    @Synchronized
    fun add(parser: ImageHeaderParser) {
        parsers.add(parser)
    }
}