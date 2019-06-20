package com.lyc.everydownload.util

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * @author liuyuchuan
 * @date 2019-06-20
 * @email kevinliu.sir@qq.com
 */
inline fun <reified T : DialogFragment> FragmentActivity.showDialog() {
    T::class.java.newInstance().show(supportFragmentManager, T::class.java.simpleName)
}

inline fun <reified T : DialogFragment> Fragment.showDialog() {
    T::class.java.newInstance().show(childFragmentManager, T::class.java.simpleName)
}
