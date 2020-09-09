package com.bumptech.glide.load.engine.prefill

import java.util.*

internal class PreFillQueue(private val bitmapsPerType: MutableMap<PreFillType, Int>) {
    private val keyList: MutableList<PreFillType>
    var size = 0
        private set
    private var keyIndex = 0
    fun remove(): PreFillType {
        val result = keyList[keyIndex]
        val countForResult = bitmapsPerType[result]
        if (countForResult == 1) {
            bitmapsPerType.remove(result)
            keyList.removeAt(keyIndex)
        } else {
            bitmapsPerType[result] = countForResult!! - 1
        }
        size--

        // Avoid divide by 0.
        keyIndex = if (keyList.isEmpty()) 0 else (keyIndex + 1) % keyList.size
        return result
    }

    val isEmpty: Boolean
        get() = size == 0

    init {
        // We don't particularly care about the initial order.
        keyList = ArrayList(bitmapsPerType.keys)
        for (count in bitmapsPerType.values) {
            size += count
        }
    }
}