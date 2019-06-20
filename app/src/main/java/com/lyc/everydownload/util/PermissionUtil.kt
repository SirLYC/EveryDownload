package com.lyc.everydownload.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.lyc.everydownload.R
import com.tbruyelle.rxpermissions2.RxPermissions

/**
 * @author liuyuchuan
 * @date 2019-06-19
 * @email kevinliu.sir@qq.com
 */


@SuppressLint("CheckResult")
inline fun FragmentActivity.doWithPermission(
        vararg permissions: String,
        crossinline grantedAction: () -> Unit,
        crossinline deniedAction: () -> Unit = {},
        crossinline shouldRationaleAction: () -> Unit = {}
) {
    RxPermissions(this).requestEachCombined(*permissions)
            .subscribe { permission ->
                when {
                    permission.granted -> grantedAction()
                    permission.shouldShowRequestPermissionRationale -> shouldRationaleAction()
                    else -> deniedAction()
                }
            }
}

@SuppressLint("CheckResult")
inline fun Fragment.doWithPermission(
        vararg permissions: String,
        crossinline grantedAction: () -> Unit,
        crossinline deniedAction: () -> Unit = {},
        crossinline shouldRationaleAction: () -> Unit = {}
) {
    activity!!.doWithPermission(*permissions, grantedAction = grantedAction, deniedAction = deniedAction, shouldRationaleAction = shouldRationaleAction)
}

inline fun FragmentActivity.doWithRWPermission(
        crossinline action: () -> Unit,
        crossinline reGrantAction: () -> Unit
) {
    doWithPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            grantedAction = action,
            shouldRationaleAction = {
                // TODO 2019-06-19 @liuyuchuan: open permission setting
                AlertDialog.Builder(this)
                        .setMessage(getString(R.string.need_rw_go_setting))
                        .setPositiveButton(R.string.file, null)
                        .show()
            },
            deniedAction = {
                AlertDialog.Builder(this)
                        .setMessage(getString(R.string.need_rw_retry))
                        .setPositiveButton(R.string.yes) { _, _ ->
                            reGrantAction()
                        }
                        .setNegativeButton(R.string.no, null)
                        .show()
            }
    )
}

inline fun Context.doWithRWPermission(
        crossinline action: () -> Unit,
        crossinline reGrantAction: () -> Unit
) {
    getFragmentActivity()?.doWithRWPermission(action, reGrantAction)
}
