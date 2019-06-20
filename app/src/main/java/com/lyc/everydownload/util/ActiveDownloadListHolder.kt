package com.lyc.everydownload.util

import androidx.annotation.MainThread
import androidx.collection.LongSparseArray
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ListUpdateCallback
import com.lyc.downloader.DownloadListener
import com.lyc.downloader.DownloadTask
import com.lyc.downloader.DownloadTask.CANCELED
import com.lyc.downloader.DownloadTask.FINISH
import com.lyc.downloader.DownloadTasksChangeListener
import com.lyc.downloader.YCDownloader
import com.lyc.downloader.db.DownloadInfo
import com.lyc.downloader.utils.DownloadStringUtil
import com.lyc.everydownload.Async
import com.lyc.everydownload.DownloadItem
import java.util.*

/**
 * Created by Liu Yuchuan on 2019/6/15.
 */
object ActiveDownloadListHolder : DownloadListener, DownloadTasksChangeListener {

    val itemList
        get() = itemListLivaData.value!!
    internal val refreshLiveDate = MutableLiveData(false)
    internal val itemListLivaData = MutableLiveData(ObservableList(ArrayList<Any>()))
    private val downloadItemList = ObservableList(ArrayList<DownloadItem>())
    private val finishedItemList = ObservableList(ArrayList<DownloadItem>())
    private val idToItem = LongSparseArray<DownloadItem>()
    private const val downloading = "正在下载"
    private const val finished = "已完成"

    private val downloadObservableListCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            var offset = 0
            val list: MutableList<Any> = ArrayList(downloadItemList.subList(position, position + count))
            if (count == downloadItemList.size) {
                list.add(0, downloading)
            } else {
                offset = 1
            }
            itemList.addAll(position + offset, list)
        }

        override fun onRemoved(position: Int, count: Int) {
            var offset = 0
            if (downloadItemList.isEmpty() && !itemList.isEmpty() && itemList[0] === downloading) {
                itemList.removeAt(0)
            } else {
                offset = 1
            }
            val p = position + offset
            var i = 0
            while (i < count) {
                itemList.removeAt(p)
                i++
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            itemList.enable = false
            itemList[fromPosition + 1] = downloadItemList[toPosition]
            itemList[toPosition + 1] = downloadItemList[fromPosition]
            itemList.enable = true
            itemList.onMoved(fromPosition + 1, toPosition + 1)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            var i = 0
            while (i < count) {
                itemList[position + i + 1] = downloadItemList[position + i]
                i++
            }
        }
    }

    private val finishedObservableListCallback = object : ListUpdateCallback {
        private val startOffset: Int
            get() = if (downloadItemList.isEmpty()) {
                0
            } else downloadItemList.size + 1

        override fun onInserted(position: Int, count: Int) {
            var offset = 0
            val list: MutableList<Any> = ArrayList(finishedItemList.subList(position, position + count))
            if (count == finishedItemList.size) {
                list.add(0, finished)
            } else {
                offset = 1
            }
            itemList.addAll(position + startOffset + offset, list)
        }

        override fun onRemoved(position: Int, count: Int) {
            var offset = startOffset
            if (finishedItemList.isEmpty() && !itemList.isEmpty() && itemList[offset] === finished) {
                itemList.removeAt(offset)
            } else {
                offset++
            }
            val p = position + offset
            var i = 0
            while (i < count) {
                itemList.removeAt(p)
                i++
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            itemList.enable = false
            itemList[fromPosition + 1 + startOffset] = downloadItemList[toPosition]
            itemList[toPosition + 1 + startOffset] = downloadItemList[fromPosition]
            itemList.enable = true
            itemList.onMoved(fromPosition + 1 + startOffset, toPosition + 1 + startOffset)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            var i = 0
            val offset = startOffset + 1
            while (i < count) {
                itemList[position + i + offset] = finishedItemList[position + i]
                i++
            }
        }
    }


    init {
        downloadItemList.addCallback(downloadObservableListCallback)
        finishedItemList.addCallback(finishedObservableListCallback)
    }

    fun setup() {
        YCDownloader.registerDownloadListener(this)
        YCDownloader.registerDownlaodTasksChangeListener(this)
        refreshList()
    }

    @MainThread
    fun refreshList() {
        if (YCDownloader.isInServerProcess()) {
            return
        }
        if (refreshLiveDate.value != false) {
            return
        }
        refreshLiveDate.postValue(true)
        Async.cache.execute {
            val downloadInfoList = YCDownloader.queryActiveDownloadInfoList()
            val finishedList = YCDownloader.queryFinishedDownloadInfoList()
            Async.main.execute {
                downloadItemList.enable = false
                finishedItemList.enable = false
                downloadItemList.clear()
                finishedItemList.clear()
                val newList = ObservableList<Any>(mutableListOf())
                if (downloadInfoList.isNotEmpty()) {
                    newList.add(downloading)
                }
                for (downloadInfo in downloadInfoList) {
                    val item = downloadInfoToItem(downloadInfo)
                    idToItem.put(downloadInfo.id!!, item)
                    downloadItemList.add(item)
                    newList.add(item)
                }

                if (finishedList.isNotEmpty()) {
                    newList.add(finished)
                }
                for (downloadInfo in finishedList) {
                    val item = downloadInfoToItem(downloadInfo)
                    idToItem.put(downloadInfo.id!!, item)
                    finishedItemList.add(item)
                    newList.add(item)
                }
                downloadItemList.enable = true
                finishedItemList.enable = true
                itemListLivaData.value = newList
                refreshLiveDate.postValue(false)
            }
        }
    }

    private fun doUpdateCallback(id: Long, updateCallback: (item: DownloadItem) -> DownloadItem) {
        val item = idToItem.get(id)?.let(updateCallback) ?: return
        val index = downloadItemList.indexOf(item)
        if (index == -1) {
            finishedItemList.remove(item)
            downloadItemList.add(0, item)
        } else if (index != -1) {
            downloadItemList[index] = item
        }
    }

    private fun <T> addToListBy(newAddItem: T, list: MutableList<T>, predicate: (T) -> Boolean) {
        var index = list.indexOfFirst(predicate)
        if (index == -1) {
            index = list.size
        }
        list.add(index, newAddItem)
    }

    override fun onNewDownloadTaskArrive(downloadInfo: DownloadInfo) {
        if (!idToItem.containsKey(downloadInfo.id) && downloadInfo.downloadItemState != CANCELED) {
            downloadInfoToItem(downloadInfo).let { newAddItem ->
                idToItem.put(downloadInfo.id, newAddItem)
                if (newAddItem.downloadState == FINISH) {
                    addToListBy(newAddItem, finishedItemList) { item ->
                        val finishTimeInList = item.finishedTime
                        val newFinishTime = newAddItem.finishedTime
                        if (finishTimeInList == null || newFinishTime == null) {
                            logE("Null finishTime when state is finish!")
                            item.createdTime <= newAddItem.createdTime
                        } else {
                            finishTimeInList <= newFinishTime
                        }
                    }
                } else {
                    addToListBy(newAddItem, downloadItemList) { item ->
                        item.createdTime <= newAddItem.createdTime
                    }
                }
            }
        }
    }

    override fun onDownloadTaskRemove(id: Long) {
        val item = idToItem.get(id)
        downloadItemList.remove(item)
        finishedItemList.remove(item)
        idToItem.remove(id)
    }

    override fun onDownloadConnecting(id: Long) {
        doUpdateCallback(id) { item ->
            item.downloadState = DownloadTask.CONNECTING
            item
        }
    }

    override fun onDownloadProgressUpdate(id: Long, total: Long, cur: Long, bps: Double) {
        doUpdateCallback(id) { item ->
            item.totalSize = total
            item.downloadedSize = cur
            item.bps = bps
            item
        }
    }

    override fun onDownloadUpdateInfo(info: DownloadInfo) {
        doUpdateCallback(info.id) {
            downloadInfoToItem(info).apply {
                idToItem.put(it.id, this)
            }
        }
    }

    override fun onDownloadError(id: Long, reason: Int, fatal: Boolean) {
        doUpdateCallback(id) { item ->
            item.downloadState = DownloadTask.ERROR
            item.errorCode = reason
            item
        }
    }

    override fun onDownloadStart(info: DownloadInfo) {
        doUpdateCallback(info.id) {
            downloadInfoToItem(info).apply {
                idToItem.put(info.id, this)
            }
        }
    }

    override fun onDownloadStopping(id: Long) {
        doUpdateCallback(id) { item ->
            item.downloadState = DownloadTask.STOPPING
            item
        }
    }

    override fun onDownloadPaused(id: Long) {
        doUpdateCallback(id) { item ->
            item.downloadState = DownloadTask.PAUSED
            item
        }
    }

    override fun onDownloadTaskWait(id: Long) {
        doUpdateCallback(id) { item ->
            item.downloadState = DownloadTask.WAITING
            item
        }
    }

    override fun onDownloadCanceled(id: Long) {
        val item = idToItem.get(id)
        downloadItemList.remove(item)
        finishedItemList.remove(item)
        idToItem.remove(id)
    }

    override fun onDownloadFinished(downloadInfo: DownloadInfo) {
        val item = idToItem.get(downloadInfo.id!!)
        if (item != null && downloadItemList.remove(item)) {
            finishedItemList.add(0, downloadInfoToItem(downloadInfo))
        }
    }

    private fun downloadInfoToItem(info: DownloadInfo): DownloadItem {
        return DownloadItem(
                info.id!!,
                info.path,
                info.filename ?: DownloadStringUtil.parseFilenameFromUrl(info.url),
                info.url,
                0.0,
                info.totalSize,
                info.downloadedSize,
                info.createdTime,
                info.finishedTime,
                info.downloadItemState,
                info.errorCode
        )
    }
}
