package com.lyc.everydownload.update

/**
 * @author liuyuchuan
 * @date 2019-07-04
 * @email kevinliu.sir@qq.com
 */
interface UpdateCheckListener {
    fun onGetUpdateInfo(info: UpdateInfo)
}

data class Progress(
        val cur: Long,
        val total: Long
)
