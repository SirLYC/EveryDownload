package com.lyc.everydownload.util

import java.io.Closeable
import java.io.Flushable
import java.net.HttpURLConnection

/**
 * @author liuyuchuan
 * @date 2019-07-04
 * @email kevinliu.sir@qq.com
 */
fun HttpURLConnection.closeQuietly() {
    try {
        disconnect()
    } catch (e: Exception) {
        logE("closeQuietly", e)
    }
}

fun Closeable.closeQuietly() {
    try {
        close()
    } catch (e: Exception) {
        logE("closeQuietly", e)
    }
}

fun Flushable.flushQuietly() {
    try {
        flush()
    } catch (e: Exception) {
        logE("flushQuietly", e)
    }
}
