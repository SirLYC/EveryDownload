package com.lyc.everydownload.util

import kotlin.math.roundToLong

/**
 * Created by Liu Yuchuan on 2019/6/17.
 */
fun Double.toTimeString(): String {
    var d = this
    if (d == Double.NaN || d == Double.POSITIVE_INFINITY ||
            d == Double.NEGATIVE_INFINITY || d == Double.MAX_VALUE) {
        return "超过1天"
    }

    if (d < 1) {
        return "即将就绪"
    }

    val hours = d / 60 / 60
    if (hours > 24) {
        return "超过一天"
    }
    val hour = (hours).roundToLong()
    if (hour > 0) {
        return "${hour}h"
    }

    d -= hour * 60 * 60
    val minute = (d / 60).roundToLong()
    if (minute > 0) {
        return "${minute}min"
    }

    val second = (d - minute * 60).roundToLong()
    if (second > 0) {
        return "${second}s"
    }

    return "即将就绪"
}
