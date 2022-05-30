package com.gaplotech.mskdemo.extensions

import com.gaplotech.mskdemo.pb.MSKDemo.Decimal
import com.gaplotech.mskdemo.pb.decimal
import java.math.BigDecimal
import java.math.RoundingMode


/**
 * Convert from Proto Decimal to java BigDecimal
 */
fun Decimal.toBigDecimal(): BigDecimal {
    return BigDecimal(unscaledVal).setScale(scale, RoundingMode.UNNECESSARY)
}

/**
 * Convert from java BigDecimal to Proto Decimal
 */
fun BigDecimal.toProtoDecimal(): Decimal {
    return decimal {
        unscaledVal = longValueExact()
        scale = scale()
    }
}

/**
 * Math '+' Ops
 */
operator fun Decimal.plus(that: Decimal): Decimal {
    return (toBigDecimal() + that.toBigDecimal()).toProtoDecimal()
}

/**
 * Math '-' Ops
 */
operator fun Decimal.minus(that: Decimal): Decimal {
    return (toBigDecimal() - that.toBigDecimal()).toProtoDecimal()
}

/**
 * Math '*' Ops
 */
operator fun Decimal.times(that: Decimal): Decimal {
    return (toBigDecimal() * that.toBigDecimal()).toProtoDecimal()
}

/**
 * Math '/' Ops
 */
operator fun Decimal.div(that: Decimal): Decimal {
    return (toBigDecimal() / that.toBigDecimal()).toProtoDecimal()
}

/**
 * Return the max between two protobuf Decimal. See {@link java.math.BigDecimal#max()}
 * @return new instance of Decimal
 */
fun Decimal.max(that: Decimal): Decimal {
    return that.toBigDecimal()
        .max(toBigDecimal())
        .toProtoDecimal()
}


/**
 * Return the min between two protobuf Decimal. See {@link java.math.BigDecimal#max()}
 * @return new instance of Decimal
 */
fun Decimal.min(that: Decimal): Decimal {
    return that.toBigDecimal()
        .min(toBigDecimal())
        .toProtoDecimal()
}
