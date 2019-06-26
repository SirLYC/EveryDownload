package com.lyc.everydownload.util.rv

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * @author liuyuchuan
 * @date 2019-06-26
 * @email kevinliu.sir@qq.com
 */
abstract class MutableViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var item: T? = null

    abstract fun onBind(oldItem: T?, newItem: T, payloads: List<Any>?)
}
