package com.lyc.everydownload

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.lyc.everydownload.util.EmptyListItem
import kotlinx.android.synthetic.main.layout_empty_default.view.*
import me.drakeet.multitype.ItemViewBinder

/**
 * Created by Liu Yuchuan on 2019/5/20.
 */
class EmptyItemViewBinder : ItemViewBinder<EmptyListItem, EmptyItemViewBinder.ViewHolder>() {
    override fun onBindViewHolder(holder: ViewHolder, item: EmptyListItem) {
        holder.text.text = item.text
        holder.text.setCompoundDrawablesRelativeWithIntrinsicBounds(0, item.icon, 0, 0)
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        val view = inflater.inflate(R.layout.layout_empty_default, parent, false)
        view.updateLayoutParams<ViewGroup.LayoutParams> {
            height = WRAP_CONTENT
        }
        return ViewHolder(view)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val text = itemView.tv_default_empty
    }
}
