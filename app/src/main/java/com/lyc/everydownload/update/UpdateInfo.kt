package com.lyc.everydownload.update

/**
 * @author liuyuchuan
 * @date 2019-07-04
 * @email kevinliu.sir@qq.com
 *
 *
 */
data class UpdateInfo(
        val code: Int,
        val name: String,
        val filename: String,
        val url: String,
        val time: Long,
        val des: String,
        val size: Long,
        val md5: String
)
