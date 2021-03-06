package com.lyc.everydownload.file

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lyc.downloader.utils.DownloadStringUtil
import com.lyc.everydownload.R
import com.lyc.everydownload.util.OnItemClickListener
import com.lyc.everydownload.util.OnItemLongClickListener
import com.lyc.everydownload.util.getIcon
import com.lyc.everydownload.util.toDateString
import kotlinx.android.synthetic.main.layout_file.view.*
import me.drakeet.multitype.ItemViewBinder
import java.io.File
import java.util.*

/**
 * @author liuyuchuan
 * @date 2019-06-20
 * @email kevinliu.sir@qq.com
 */
class FileItemViewBinder(
        private val onItemClickListener: OnItemClickListener<File>,
        private val onItemLongClickListener: OnItemLongClickListener<File>
) : ItemViewBinder<File, FileItemViewBinder.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, item: File) {
        holder.bind(item)
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(
                inflater.inflate(R.layout.layout_file, parent, false),
                onItemClickListener,
                onItemLongClickListener
        )
    }

    class ViewHolder(
            itemView: View,
            private val onItemClickListener: OnItemClickListener<File>,
            private val onItemLongClickListener: OnItemLongClickListener<File>
    ) : RecyclerView.ViewHolder(itemView) {
        private var file: File? = null
        private val icon = itemView.iv_icon
        private val filename = itemView.tv_filename
        private val extraInfo = itemView.tv_extra_info

        init {
            itemView.setOnClickListener { view ->
                file?.let { file ->
                    onItemClickListener.onItemClick(view, file, adapterPosition)
                }
            }

            itemView.setOnLongClickListener { view ->
                file?.let { file ->
                    onItemLongClickListener.onItemLongClick(view, file, adapterPosition)
                } ?: false
            }
        }

        internal fun bind(file: File) {
            this.file = file
            if (file.isFile) {
                extraInfo.visibility = VISIBLE
                extraInfo.text = ("${DownloadStringUtil.byteToString(file.length().toDouble())} | ${Date(file.lastModified()).toDateString()}")
            } else {
                extraInfo.visibility = GONE
            }

            filename.text = file.name
            icon.setImageDrawable(ContextCompat.getDrawable(icon.context, file.getIcon()))
        }
    }
}
