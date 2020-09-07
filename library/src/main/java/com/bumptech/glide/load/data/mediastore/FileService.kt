package com.bumptech.glide.load.data.mediastore

import java.io.File

internal class FileService {
    fun exists(file: File): Boolean = file.exists()

    fun length(file: File): Long = file.length()

    operator fun get(path: String) = File(path)
}