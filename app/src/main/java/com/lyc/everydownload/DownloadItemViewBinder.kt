package com.lyc.everydownload

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lyc.downloader.DownloadTask.*
import com.lyc.downloader.YCDownloader
import com.lyc.downloader.utils.DownloadStringUtil
import com.lyc.everydownload.util.toDateString
import com.lyc.everydownload.util.toTimeString
import kotlinx.android.synthetic.main.item_download.view.*
import me.drakeet.multitype.ItemViewBinder
import java.io.File

/**
 * Created by Liu Yuchuan on 2019/5/20.
 */
class DownloadItemViewBinder(
        private val onItemButtonClickListener: OnItemButtonClickListener
) : ItemViewBinder<DownloadItem, DownloadItemViewBinder.ViewHolder>() {
    override fun onBindViewHolder(holder: ViewHolder, item: DownloadItem) {
        holder.bind(item)
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.item_download, parent, false), onItemButtonClickListener)
    }

    class ViewHolder(
            itemView: View,
            private val onItemButtonClickListener: OnItemButtonClickListener
    ) :
            RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

        override fun onLongClick(v: View) = item?.let {
            onItemButtonClickListener.onItemLongClicked(it, v)
        } ?: false

        private val name: TextView = itemView.name
        private val state: TextView = itemView.state
        private val oldStateColors = state.textColors
        private val button: ImageButton = itemView.button.apply {
            setOnClickListener(this@ViewHolder)
        }
        private val downloadProgressBar = itemView.progress_bar.apply {
            max = 100
        }

        init {
            itemView.setOnLongClickListener(this)
            itemView.setOnClickListener(this)
        }

        private var item: DownloadItem? = null

        internal fun bind(item: DownloadItem) {
            this.item = item
            name.text = item.filename
            val progress: Int
            val cur = item.downloadedSize
            val total = item.totalSize.toDouble()

            val downloadState = item.downloadState
            var stateString: String?
            if (total <= 0 || downloadState == FINISH) {
                progress = 0
                stateString = DownloadStringUtil.byteToString(cur.toDouble())
            } else {
                progress = Math.max((cur / total * 100).toInt(), 0)
                stateString = DownloadStringUtil.byteToString(cur.toDouble())
                if (total > 0) {
                    stateString += "/${DownloadStringUtil.byteToString(total)}"
                }
            }

            downloadProgressBar.progress = progress

            when (item.downloadState) {
                PENDING, CONNECTING -> {
                    stateString = "$stateString | 连接中"
                }
                RUNNING -> {
                    val leftTimeString =
                            if (total > 0 && item.bps > 0) {
                                " | ${(total / item.bps).toTimeString()}"
                            } else if (total > 0) {
                                " | 超过一天"
                            } else {
                                ""
                            }
                    stateString = "$stateString | ${DownloadStringUtil.bpsToString(item.bps)}$leftTimeString"
                }
                STOPPING -> {
                    stateString = "$stateString | 正在停止"
                }
                PAUSED -> stateString = "$stateString | 已暂停"
                WAITING -> stateString = "$stateString | 等待中"
                CANCELED -> stateString = "已取消"
                ERROR, FATAL_ERROR -> {
                    var errorMessage: String? = YCDownloader.translateErrorCode(item.errorCode!!)
                    if (errorMessage == null) {
                        errorMessage = "下载失败"
                    }
                    stateString = errorMessage
                }
            }

            downloadProgressBar.isIndeterminate = (total <= 0 && (downloadState == RUNNING || downloadState == CONNECTING))

            button.isEnabled = downloadState != STOPPING
            if (item.downloadState == FINISH) {
                button.setImageDrawable(ContextCompat
                        .getDrawable(button.context, R.drawable.ic_folder_open_primary_24dp))
                item.finishedTime?.let {
                    stateString = "$stateString | ${it.toDateString()}"
                }
                state.text = stateString
            } else {
                state.text = stateString
                if (downloadState == PENDING
                        || downloadState == RUNNING
                        || downloadState == CONNECTING
                        || downloadState == WAITING) {
                    button.setImageDrawable(ContextCompat
                            .getDrawable(button.context, R.drawable.ic_pause_primary_24dp))
                } else {
                    button.setImageDrawable(ContextCompat
                            .getDrawable(button.context, R.drawable.ic_file_download_primary_24dp))
                }
            }

            downloadProgressBar.visibility = when (downloadState) {
                FINISH, ERROR, FATAL_ERROR -> GONE
                else -> VISIBLE
            }

            if (downloadState == ERROR || downloadState == FATAL_ERROR) {
                state.setTextColor(ContextCompat.getColor(state.context, R.color.error))
            } else {
                state.setTextColor(oldStateColors)
            }
        }

        override fun onClick(v: View) {
            item?.let {
                val state = it.downloadState
                if (v.id == R.id.button || v == itemView) {
                    when (state) {
                        FINISH -> {
                            val name = it.filename
                            if (File(it.path, name).exists()) {
                                onItemButtonClickListener.openItemFile(it)
                            } else {
                                onItemButtonClickListener.openItemFileNotExist(it)
                            }
                        }
                        PAUSED, ERROR, FATAL_ERROR -> onItemButtonClickListener.startItem(it)
                        RUNNING, CONNECTING, WAITING -> onItemButtonClickListener.pauseItem(it)
                    }
                }
            }
        }

    }

    interface OnItemButtonClickListener {
        fun openItemFile(item: DownloadItem)

        fun openItemFileNotExist(item: DownloadItem)

        fun pauseItem(item: DownloadItem)

        fun startItem(item: DownloadItem)

        fun onItemLongClicked(item: DownloadItem, view: View): Boolean
    }
}
