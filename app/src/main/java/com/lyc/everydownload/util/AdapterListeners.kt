package com.lyc.everydownload.util

import android.view.View

/**
 * @author liuyuchuan
 * @date 2019-06-20
 * @email kevinliu.sir@qq.com
 */
interface OnItemClickListener<T> {
    fun onItemClick(v: View, value: T, index: Int)
}

interface OnItemLongClickListener<T> {
    fun onItemLongClick(v: View, value: T, index: Int): Boolean
}
