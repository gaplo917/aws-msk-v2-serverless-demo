package com.gaplotech.mskdemo.extensions

import com.typesafe.config.Config
import java.util.*

fun Config.toProperties(): Properties {
    val entries = this.entrySet()
    return Properties().apply {
        entries.forEach {
            put(it.key, it.value.unwrapped())
        }
    }
}