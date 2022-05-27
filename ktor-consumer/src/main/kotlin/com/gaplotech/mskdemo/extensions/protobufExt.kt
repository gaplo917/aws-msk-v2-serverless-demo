package com.gaplotech.mskdemo.extensions

import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat

fun Message.toJsonString(): String {
    return JsonFormat
        .printer()
        .omittingInsignificantWhitespace()
        .print(this)
}