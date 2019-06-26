package com.lyc.everydownload.util.rv

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * @author liuyuchuan
 * @date 2019-06-17
 * @email kevinliu.sir@qq.com
 */
class VerticalItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View,
                                parent: RecyclerView, state: RecyclerView.State) {
        outRect.bottom = space
    }
}
