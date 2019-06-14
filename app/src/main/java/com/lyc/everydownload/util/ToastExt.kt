package com.lyc.everydownload.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

/**
 * @author liuyuchuan
 * @date 2019-06-12
 * @email kevinliu.sir@qq.com
 */
fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Fragment.toast(msg: String) {
    context?.let {
        Toast.makeText(it, msg, Toast.LENGTH_SHORT).show()
    }
}

fun Context.toast(@StringRes msgResId: Int) {
    Toast.makeText(this, msgResId, Toast.LENGTH_SHORT).show()
}

fun Fragment.toast(@StringRes msgResId: Int) {
    context?.let {
        Toast.makeText(it, msgResId, Toast.LENGTH_SHORT).show()
    }
}
