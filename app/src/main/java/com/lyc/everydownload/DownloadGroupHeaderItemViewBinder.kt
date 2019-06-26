package com.lyc.everydownload

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lyc.everydownload.util.DownloadGroupHeader
import com.lyc.everydownload.util.OnItemClickListener
import kotlinx.android.synthetic.main.item_group_header.view.*
import me.drakeet.multitype.ItemViewBinder
import kotlin.math.roundToLong

/**
 * Created by Liu Yuchuan on 2019/5/20.
 */
class DownloadGroupHeaderItemViewBinder(
        private val onItemClickListener: OnItemClickListener<DownloadGroupHeader>
) : ItemViewBinder<DownloadGroupHeader, DownloadGroupHeaderItemViewBinder.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, item: DownloadGroupHeader) {
        holder.bind(item, null)
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        println("create!")
        return ViewHolder(inflater.inflate(R.layout.item_group_header, parent, false), onItemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: DownloadGroupHeader, payloads: List<Any>) {
        holder.bind(item, payloads)
    }

    class ViewHolder(
            itemView: View,
            private val onItemClickListener: OnItemClickListener<DownloadGroupHeader>
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        override fun onClick(v: View?) {
            if (v == text) {
                downloadGroupHeader?.let {
                    onItemClickListener.onItemClick(text, it, adapterPosition)
                }
            }
        }

        val text: TextView = itemView.tv_group.also {
            it.setOnClickListener(this)
        }

        private val defaultTextColor: ColorStateList = text.textColors
        private val primaryColor = ContextCompat.getColor(itemView.context, R.color.colorPrimary)
        private var downloadGroupHeader: DownloadGroupHeader? = null
        // mutate!!!
        private val endDrawable = (ContextCompat.getDrawable(itemView.context, R.drawable.expand_arrow_anim))!!.mutate()

        private val rotateTime = 120

        val expandAnim: ObjectAnimator = ObjectAnimator.ofInt(endDrawable, "level", 0, 10000).apply {
            setAutoCancel(true)
            doOnEnd {
                text.setTextColor(primaryColor)
            }
        }

        val collapseAnim: ObjectAnimator = ObjectAnimator.ofInt(endDrawable, "level", 10000, 0).apply {
            setAutoCancel(true)
            doOnEnd {
                text.setTextColor(defaultTextColor)
            }
        }

        fun bind(item: DownloadGroupHeader, payload: List<Any>?) {
            val updateAll = downloadGroupHeader != item || payload == null || payload.isEmpty()
            this.downloadGroupHeader = item

            if (updateAll) {
                if (item.expand) {
                    text.setTextColor(primaryColor)
                    endDrawable.level = 10000
                } else {
                    text.setTextColor(defaultTextColor)
                    endDrawable.level = 0
                }
                val startIcon = ContextCompat.getDrawable(itemView.context, item.icon)
                text.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        startIcon, null, endDrawable, null)
                text.text = ("${item.text} (${item.count})")
            } else {
                payload!!.forEach {
                    when (it) {
                        DownloadGroupHeader.UPDATE_EXPAND -> {
                            if (item.expand) {
                                if (!expandAnim.isRunning) {
                                    expandAnim.setIntValues(endDrawable.level, 10000)
                                    expandAnim.duration = ((10000 - endDrawable.level) / 10000f * rotateTime).roundToLong()
                                    expandAnim.start()
                                }
                            } else {
                                if (!collapseAnim.isRunning) {
                                    expandAnim.duration = (endDrawable.level / 10000f * rotateTime).roundToLong()
                                    collapseAnim.setIntValues(endDrawable.level, 0)
                                    collapseAnim.start()
                                }
                            }
                        }

                        DownloadGroupHeader.UPDATE_COUNT -> {
                            text.text = ("${item.text}(${item.count})")
                        }
                    }
                }
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.collapseAnim.cancel()
        holder.expandAnim.cancel()
    }
}
