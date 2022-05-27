package com.gaplotech.mskdemo.extensions

import java.text.SimpleDateFormat
import java.util.*

fun Long.toDateTimeString(): String {
    val format = "yyyy-MM-dd" // you can add the format you need
    val sdf = SimpleDateFormat(format, Locale.US) // default local
    sdf.timeZone = TimeZone.getTimeZone("HKT") // set anytime zone you need
    return sdf.format(this)
}