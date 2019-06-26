package com.lyc.everydownload.util

import androidx.annotation.DrawableRes

/**
 * @author liuyuchuan
 * @date 2019-06-26
 * @email kevinliu.sir@qq.com
 */
class DownloadGroupHeader(
        val id: Int,
        val text: String,
        @DrawableRes val icon: Int,
        var expand: Boolean
) {
    var count = 0

    companion object {
        const val UPDATE_COUNT = 0
        const val UPDATE_EXPAND = 1
    }
}
