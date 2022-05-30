package com.gaplotech.mskdemo.extensions

import com.gaplotech.mskdemo.pb.MSKDemo
import com.gaplotech.mskdemo.pb.MSKDemoWS
import com.gaplotech.mskdemo.pb.candleStickWebSocketResponse
import com.gaplotech.mskdemo.pb.slidingWebSocketResponse
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat

fun Message.toJsonString(): String {
    return JsonFormat
        .printer()
        .omittingInsignificantWhitespace()
        .print(this)
}

fun MSKDemo.CandleStick.toWebSocketResponse(): MSKDemoWS.CandleStickWebSocketResponse {
    val self = this
    return candleStickWebSocketResponse {
        instrumentId = self.instrumentId
        volume = self.volume.toBigDecimal().toString()
        open = self.open.toBigDecimal().toString()
        close = self.close.toBigDecimal().toString()
        high = self.high.toBigDecimal().toString()
        low = self.low.toBigDecimal().toString()
        count = self.count
        startTime = self.startTime
        endTime = self.endTime
    }
}

fun MSKDemo.SlidingAggregate.toWebSocketResponse(): MSKDemoWS.SlidingWebSocketResponse {
    val self = this
    return slidingWebSocketResponse {
        instrumentId = self.instrumentId
        volume = self.volume.toBigDecimal().toString()
        priceChange = self.priceChange.toBigDecimal().toString()
        open = self.open.toBigDecimal().toString()
        close = self.close.toBigDecimal().toString()
        high = self.high.toBigDecimal().toString()
        low = self.low.toBigDecimal().toString()
        count = self.count
        startTime = self.startTime
        endTime = self.endTime
    }
}
