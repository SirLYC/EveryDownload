package com.lyc.everydownload

import androidx.lifecycle.ViewModel
import com.lyc.downloader.SubmitListener
import com.lyc.downloader.YCDownloader
import com.lyc.downloader.db.DownloadInfo
import com.lyc.everydownload.util.SingleLiveEvent
import com.lyc.everydownload.util.logD

/**
 * @author liuyuchuan
 * @date 2019-04-23
 * @email kevinliu.sir@qq.com
 */
class MainViewModel : ViewModel(), SubmitListener {
    internal val failLiveData = SingleLiveEvent<String>()

    internal fun submit(url: String, path: String, filename: String?) {
        YCDownloader.submit(url, path, filename, this)
    }

    override fun submitSuccess(downloadInfo: DownloadInfo) {
        logD("submitSuccess: ${downloadInfo.url} | ${downloadInfo.path} | ${downloadInfo.filename}")
    }

    override fun submitFail(e: Exception) {
        e.printStackTrace()
        var message = e.message ?: ""
        if (message.length > 20) {
            message = message.substring(0, 17) + "..."
        }
        failLiveData.postValue("创建失败[$message]")
    }

    internal fun pause(id: Long) {
        YCDownloader.pause(id)
    }

    internal fun start(id: Long) {
        YCDownloader.startOrResume(id, false)
    }

    internal fun cancel(id: Long) {
        YCDownloader.cancel(id)
    }

    internal fun delete(id: Long, deleteFile: Boolean) {
        YCDownloader.delete(id, deleteFile)
    }

    internal fun restart(id: Long) {
        YCDownloader.startOrResume(id, true)
    }
}
