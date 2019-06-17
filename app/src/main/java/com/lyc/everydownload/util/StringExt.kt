package com.lyc.everydownload.util

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong

/**
 * Created by Liu Yuchuan on 2019/6/17.
 */

private val totalFmt = SimpleDateFormat("yyyy-MM-dd HH:mm")
private val todayFmt = SimpleDateFormat("HH:mm")
private val yesterdayFmt = SimpleDateFormat("昨天 HH:mm")
private val thisYearFmt = SimpleDateFormat("MM-dd HH:mm")


fun Double.toTimeString(): String {
    var d = this
    if (d == Double.NaN || d == Double.POSITIVE_INFINITY ||
            d == Double.NEGATIVE_INFINITY || d == Double.MAX_VALUE) {
        return "下载中"
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


fun CharSequence.toNormalUrl(): CharSequence {
    var url = this
    val oldUrlPrefix = if (url.length > 8) {
        url.substring(0, 8)
    } else {
        url
    }.toString().toLowerCase()

    if (!oldUrlPrefix.startsWith("http://") && !oldUrlPrefix.startsWith("https://")) {
        val prefix = when {
            oldUrlPrefix.startsWith("http:") -> {
                val sb = StringBuilder(url.removeRange(0, 5))
                while (sb.startsWith("/")) {
                    sb.removeRange(0, 1)
                }
                url = sb
                "http://"
            }
            oldUrlPrefix.startsWith("https:") -> {
                val sb = StringBuilder(url.removeRange(0, 6))
                while (sb.startsWith("/")) {
                    sb.removeRange(0, 1)
                }
                url = sb
                "https://"
            }
            else -> "http://"
        }
        return "$prefix$url"
    }

    return url
}

fun Date.toDateString(): String {
    val now = Calendar.getInstance()
    val toFormat = Calendar.getInstance().also {
        it.timeInMillis = this.time
    }
    if (now.get(Calendar.YEAR) != toFormat.get(Calendar.YEAR)) {
        return totalFmt.format(this)
    }

    val dayOfYearNow = now.get(Calendar.DAY_OF_YEAR)
    val dayOfYearToFormat = toFormat.get(Calendar.DAY_OF_YEAR)

    if (dayOfYearNow == dayOfYearToFormat) {
        return todayFmt.format(this)
    }

    toFormat.add(Calendar.DAY_OF_YEAR, 1)
    if (dayOfYearNow == toFormat.get(Calendar.DAY_OF_YEAR)) {
        return yesterdayFmt.format(this)
    }

    return thisYearFmt.format(this)
}
