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
import com.lyc.everydownload.App
import com.lyc.everydownload.Async
import com.lyc.everydownload.DownloadItem
import com.lyc.everydownload.R
import com.lyc.everydownload.util.rv.ObservableList
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
    private var downloadingHeader = DownloadGroupHeader(ID_DOWNLOADING, App.appContext.getString(R.string.title_downloading), R.drawable.ic_file_download_gray_24dp, true)
    private var finishedHeader = DownloadGroupHeader(ID_FINISHED, App.appContext.getString(R.string.title_finished), R.drawable.ic_view_list_grey_24dp, true)
    private val downloadEmptyItem = EmptyListItem(ID_DOWNLOADING, App.appContext.getString(R.string.empty_downloading_list), R.drawable.ic_empty_download)
    private val finishedEmptyItem = EmptyListItem(ID_FINISHED, App.appContext.getString(R.string.advice_to_to_download), R.drawable.ic_empty_box)
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
            itemList.disableCallback {
                set(fromPosition + 1, downloadingItemList[toPosition])
                set(toPosition + 1, downloadingItemList[fromPosition])
            }
            itemList.onMoved(fromPosition + 1, toPosition + 1)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            var i = 0
            itemList.disableCallback {
                while (i < count) {
                    set(position + i + 1, downloadingItemList[position + i])
                    i++
                }
            }
            itemList.onChanged(position + 1, count, payload)
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
            itemList.disableCallback {
                set(fromPosition + offset, downloadingItemList[toPosition])
                set(toPosition + offset, fromPosition)
            }
            itemList.onMoved(fromPosition + offset, toPosition + offset)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            var i = 0
            val offset = finishedListStartOffset
            itemList.disableCallback {
                while (i < count) {
                    set(position + i + offset, finishedItemList[position + i])
                    i++
                }
            }
            itemList.onChanged(position + offset, count, payload)
        }
    }


    init {
        downloadingItemList.addCallback(downloadingListCallback)
        finishedItemList.addCallback(finishedObservableListCallback)
    }

    fun setup() {
        YCDownloader.registerDownloadListener(this)
        YCDownloader.registerDownloadTasksChangeListener(this)
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
                val oldIdToItem = idToItem.clone()
                idToItem.clear()
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

                for (downloadInfo in downloadInfoList) {
                    val item = downloadInfoToItem(downloadInfo)
                    oldIdToItem[item.id]?.run {
                        item.bps = bps
                    }
                    idToItem.put(downloadInfo.id!!, item)
                    downloadingItemList.add(item)
                }
                if (expandDownloading) {
                    if (downloadInfoList.isEmpty()) {
                        newList.add(downloadEmptyItem)
                    } else {
                        newList.addAll(downloadingItemList)
                    }
                    downloadingItemList.enable = true
                }

                finishedHeader = DownloadGroupHeader(ID_FINISHED, "已完成",
                        R.drawable.ic_view_list_grey_24dp, expandFinished).also {
                    it.count = finishedList.size
                    newList.add(it)
                }
                for (downloadInfo in finishedList) {
                    val item = downloadInfoToItem(downloadInfo)
                    oldIdToItem[item.id]?.run {
                        item.bps = bps
                    }
                    idToItem.put(downloadInfo.id!!, item)
                    finishedItemList.add(item)
                }
                if (expandFinished) {
                    if (finishedList.isEmpty()) {
                        newList.add(finishedEmptyItem)
                    } else {
                        newList.addAll(finishedItemList)
                    }
                    finishedItemList.enable = true
                }

                itemListLivaData.value = newList
                refreshLiveDate.value = false
            }
        }
    }

    private inline fun doUpdateCallback(id: Long, updateType: Int? = null, updateCallback: (item: DownloadItem) -> DownloadItem) {
        val oldItem = idToItem.get(id)
        val item = oldItem?.let(updateCallback) ?: return
        val index = downloadingItemList.indexOf(item)
        if (index == -1 && item.downloadState != FINISH) {
            if (finishedItemList.remove(item)) {
                finishedHeader.count--
                itemList.onChanged(finishedListStartOffset - 1, 1, DownloadGroupHeader.UPDATE_COUNT)
            }
            addToListBy(item, downloadingItemList, {
                item.createdTime <= item.createdTime
            })
            downloadingHeader.count++
            itemList.onChanged(0, 1, DownloadGroupHeader.UPDATE_COUNT)
        } else if (index != -1) {
            downloadingItemList.disableCallback {
                if (!(oldItem === item)) {
                    set(index, item)
                }
            }
            downloadingItemList.onChanged(index, 1, updateType)
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
        doUpdateCallback(id, DownloadItem.UPDATE_PROGRESS) { item ->
            item.totalSize = total
            item.downloadedSize = cur
            item.bps = bps
            item
        }
    }

    override fun onDownloadUpdateInfo(info: DownloadInfo) {
        doUpdateCallback(info.id, DownloadItem.UPDATE_INFO) {
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

    fun hasAnyDownloadingTask() = downloadingItemList.isNotEmpty()
}
