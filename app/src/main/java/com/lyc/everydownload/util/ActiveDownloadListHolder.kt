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
import com.lyc.everydownload.R
import java.util.*

/**
 * Created by Liu Yuchuan on 2019/6/15.
 */
object ActiveDownloadListHolder : DownloadListener, DownloadTasksChangeListener {
    const val ID_DOWNLOADING = 0
    const val ID_FINISHED = 1

    val itemList
        get() = itemListLivaData.value!!
    internal val refreshLiveDate = MutableLiveData(false)
    internal val itemListLivaData = MutableLiveData(ObservableList(ArrayList<Any>()))
    private val downloadingItemList = ObservableList(ArrayList<DownloadItem>())
    private val finishedItemList = ObservableList(ArrayList<DownloadItem>())
    private val idToItem = LongSparseArray<DownloadItem>()
    private var downloadingHeader = DownloadGroupHeader(ID_DOWNLOADING, "正在下载", R.drawable.ic_file_download_gray_24dp, true)
    private var finishedHeader = DownloadGroupHeader(ID_FINISHED, "已完成", R.drawable.ic_view_list_grey_24dp, true)
    private val downloadEmptyItem = EmptyListItem(ID_DOWNLOADING, "还没有任何下载记录～", R.drawable.ic_empty_download)
    private val finishedEmptyItem = EmptyListItem(ID_FINISHED, "还没有任何下载记录～", R.drawable.ic_empty_box)
    internal var expandDownloading = true
        set(value) {
            if (field != value) {
                field = value
                downloadingHeader.expand = value
                itemList.onChanged(0, 1, DownloadGroupHeader.UPDATE_EXPAND)
                downloadingItemList.enable = value
                if (value) {
                    if (downloadingItemList.isEmpty()) {
                        itemList.add(1, downloadEmptyItem)
                    } else {
                        itemList.addAll(1, downloadingItemList)
                    }
                } else {
                    if (downloadingItemList.isEmpty()) {
                        itemList.removeAt(1)
                    } else {
                        itemList.removeRange(1, downloadingItemList.size)
                    }
                }
            }
        }

    internal var expandFinished = true
        set(value) {
            if (field != value) {
                field = value
                finishedHeader.expand = value
                val offset = finishedListStartOffset
                itemList.onChanged(offset - 1, 1, DownloadGroupHeader.UPDATE_EXPAND)
                finishedItemList.enable = value
                if (value) {
                    if (finishedItemList.isEmpty()) {
                        itemList.add(offset, finishedEmptyItem)
                    } else {
                        itemList.addAll(offset, finishedItemList)
                    }
                } else {
                    if (finishedItemList.isEmpty()) {
                        itemList.removeAt(offset)
                    } else {
                        itemList.removeRange(offset, finishedItemList.size)
                    }
                }
            }
        }


    private val finishedListStartOffset
        get() = if (!expandDownloading) {
            2
        } else if (downloadingItemList.isEmpty()) {
            3
        } else {
            downloadingItemList.size + 2
        }

    private val downloadingListCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            if (count == downloadingItemList.size) {
                itemList.removeAt(1)
            }
            itemList.addAll(position + 1, downloadingItemList.subList(position, position + count))
        }

        override fun onRemoved(position: Int, count: Int) {
            itemList.removeRange(position + 1, count)
            if (downloadingItemList.isEmpty()) {
                itemList.add(1, downloadEmptyItem)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            itemList.enable = false
            itemList[fromPosition + 1] = downloadingItemList[toPosition]
            itemList[toPosition + 1] = downloadingItemList[fromPosition]
            itemList.enable = true
            itemList.onMoved(fromPosition + 1, toPosition + 1)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            var i = 0
            while (i < count) {
                itemList[position + i + 1] = downloadingItemList[position + i]
                i++
            }
        }
    }

    private val finishedObservableListCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            val offset = finishedListStartOffset
            if (count == finishedItemList.size) {
                itemList.removeAt(offset)
            }
            itemList.addAll(position + offset, finishedItemList.subList(position, position + count))
        }

        override fun onRemoved(position: Int, count: Int) {
            val offset = finishedListStartOffset
            itemList.removeRange(position + offset, count)
            if (finishedItemList.isEmpty()) {
                itemList.add(offset, finishedEmptyItem)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            val offset = finishedListStartOffset
            itemList.enable = false
            itemList[fromPosition + offset] = downloadingItemList[toPosition]
            itemList[toPosition + offset] = downloadingItemList[fromPosition]
            itemList.enable = true
            itemList.onMoved(fromPosition + offset, toPosition + offset)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            var i = 0
            val offset = finishedListStartOffset
            while (i < count) {
                itemList[position + i + offset] = finishedItemList[position + i]
                i++
            }
        }
    }


    init {
        downloadingItemList.addCallback(downloadingListCallback)
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
                downloadingItemList.enable = false
                finishedItemList.enable = false
                downloadingItemList.clear()
                finishedItemList.clear()
                val newList = ObservableList<Any>(mutableListOf())
                downloadingHeader = DownloadGroupHeader(ID_DOWNLOADING, "正在下载",
                        R.drawable.ic_file_download_gray_24dp, expandDownloading).also {
                    it.count = downloadInfoList.size
                    newList.add(it)
                }
                if (expandDownloading) {
                    if (downloadInfoList.isEmpty()) {
                        newList.add(downloadEmptyItem)
                    } else {
                        for (downloadInfo in downloadInfoList) {
                            val item = downloadInfoToItem(downloadInfo)
                            idToItem.put(downloadInfo.id!!, item)
                            downloadingItemList.add(item)
                            newList.add(item)
                        }
                    }
                    downloadingItemList.enable = true
                }

                finishedHeader = DownloadGroupHeader(ID_FINISHED, "已完成",
                        R.drawable.ic_view_list_grey_24dp, expandFinished).also {
                    it.count = finishedList.size
                    newList.add(it)
                }
                if (expandFinished) {
                    if (finishedList.isEmpty()) {
                        newList.add(finishedEmptyItem)
                    } else {
                        for (downloadInfo in finishedList) {
                            val item = downloadInfoToItem(downloadInfo)
                            idToItem.put(downloadInfo.id!!, item)
                            finishedItemList.add(item)
                            newList.add(item)
                        }
                    }
                    finishedItemList.enable = true
                }

                itemListLivaData.value = newList
                refreshLiveDate.value = false
            }
        }
    }

    private fun doUpdateCallback(id: Long, updateCallback: (item: DownloadItem) -> DownloadItem) {
        val item = idToItem.get(id)?.let(updateCallback) ?: return
        val index = downloadingItemList.indexOf(item)
        if (index == -1) {
            finishedItemList.remove(item)
            downloadingItemList.add(0, item)
        } else if (index != -1) {
            downloadingItemList[index] = item
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
                    finishedHeader.count++
                    itemList.onChanged(finishedListStartOffset - 1, 1, DownloadGroupHeader.UPDATE_COUNT)
                } else {
                    addToListBy(newAddItem, downloadingItemList) { item ->
                        item.createdTime <= newAddItem.createdTime
                    }
                    downloadingHeader.count++
                    itemList.onChanged(0, 1, DownloadGroupHeader.UPDATE_COUNT)
                }
            }
        }
    }

    override fun onDownloadTaskRemove(id: Long) {
        val item = idToItem.get(id)
        if (downloadingItemList.remove(item)) {
            downloadingHeader.count--
            itemList.onChanged(0, 1, DownloadGroupHeader.UPDATE_COUNT)
        }
        if (finishedItemList.remove(item)) {
            finishedHeader.count--
            itemList.onChanged(finishedListStartOffset - 1, 1, DownloadGroupHeader.UPDATE_COUNT)
        }
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
        onDownloadTaskRemove(id)
    }

    override fun onDownloadFinished(downloadInfo: DownloadInfo) {
        val item = idToItem.get(downloadInfo.id!!)
        if (item != null && downloadingItemList.remove(item)) {
            finishedItemList.add(0, downloadInfoToItem(downloadInfo))
            downloadingHeader.count--
            finishedHeader.count++
            itemList.onChanged(0, 1, DownloadGroupHeader.UPDATE_COUNT)
            itemList.onChanged(finishedListStartOffset - 1, 1, DownloadGroupHeader.UPDATE_COUNT)
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
