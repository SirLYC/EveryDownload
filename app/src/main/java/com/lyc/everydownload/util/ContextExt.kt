package com.lyc.everydownload.util

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * @author liuyuchuan
 * @date 2019-06-20
 * @email kevinliu.sir@qq.com
 */
fun Context.getFragmentActivity(): FragmentActivity? {
    var cur: Context? = this

    while (cur != null && cur !is FragmentActivity && cur is ContextWrapper) {
        cur = cur.baseContext
    }

    return cur as? FragmentActivity
}
