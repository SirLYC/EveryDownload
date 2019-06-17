package com.lyc.everydownload.util

import com.lyc.downloader.YCDownloader

/**
 * Created by Liu Yuchuan on 2019/6/15.
 */
object ActiveDownloadListHolder {

    fun refreshList() {
        if (YCDownloader.isInServerProcess()) {
            return
        }

    }
}
