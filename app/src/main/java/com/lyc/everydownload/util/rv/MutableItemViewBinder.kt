package com.lyc.everydownload.util.rv

import me.drakeet.multitype.ItemViewBinder

/**
 * @author liuyuchuan
 * @date 2019-06-26
 * @email kevinliu.sir@qq.com
 */
abstract class MutableItemViewBinder<T, VH : MutableViewHolder<T>> : ItemViewBinder<T, VH>() {
    final override fun onBindViewHolder(holder: VH, item: T) {
        val oldItem = holder.item
        holder.item = item
        holder.onBind(oldItem, item, null)
    }

    final override fun onBindViewHolder(holder: VH, item: T, payloads: List<Any>) {
        val oldItem = holder.item
        holder.item = item
        holder.onBind(oldItem, item, payloads)
    }
}


