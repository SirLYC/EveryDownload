package com.lyc.everydownload

import java.util.*

/**
 * @author liuyuchuan
 * @date 2019-04-23
 * @email kevinliu.sir@qq.com
 */
data class DownloadItem(
        var id: Long = 0,
        var path: String,
        var filename: String,
        var url: String,
        var bps: Double = 0.toDouble(),
        var totalSize: Long = 0,
        var downloadedSize: Long = 0,
        var createdTime: Date,
        var finishedTime: Date? = null,
        var downloadState: Int = 0,
        var errorCode: Int? = null
) {

    override fun equals(other: Any?): Boolean {
        return this.id == (other as? DownloadItem)?.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        fun itemWithId(id: Long) = DownloadItem(
                id,
                "",
                "," +
                        null,
                "",
                createdTime = Date()
        )
    }
}
