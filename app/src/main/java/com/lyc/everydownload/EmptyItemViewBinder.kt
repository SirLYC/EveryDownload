package com.lyc.everydownload

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.updateLayoutParams
import com.lyc.everydownload.util.EmptyListItem
import com.lyc.everydownload.util.rv.MutableItemViewBinder
import com.lyc.everydownload.util.rv.MutableViewHolder
import kotlinx.android.synthetic.main.layout_empty_default.view.*

/**
 * Created by Liu Yuchuan on 2019/5/20.
 */
class EmptyItemViewBinder : MutableItemViewBinder<EmptyListItem, EmptyItemViewBinder.ViewHolder>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        val view = inflater.inflate(R.layout.layout_empty_default, parent, false)
        view.updateLayoutParams<ViewGroup.LayoutParams> {
            height = WRAP_CONTENT
        }
        return ViewHolder(view)
    }

    class ViewHolder(itemView: View) : MutableViewHolder<EmptyListItem>(itemView) {
        internal val text = itemView.tv_default_empty

        override fun onBind(oldItem: EmptyListItem?, newItem: EmptyListItem, payloads: List<Any>?) {
            if (oldItem?.text != newItem.text) {
                text.text = newItem.text
                text.setCompoundDrawablesRelativeWithIntrinsicBounds(0, newItem.icon, 0, 0)
            }
        }
    }
}
