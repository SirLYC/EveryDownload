package com.lyc.everydownload.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import com.lyc.everydownload.R

/**
 * Created by Liu Yuchuan on 2019/6/30.
 */
inline fun Context.copyPlain(text: String, successAction: () -> Unit = {}, failedAction: () -> Unit = {}) {
    val clipboardService = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    if (clipboardService != null) {
        clipboardService.primaryClip = ClipData.newPlainText(null, text)
        successAction()
    } else {
        failedAction()
    }
}

fun View.copyPlainWithSnackBarTip(text: String) {
    val ctx = context
    ctx.copyPlain(text, successAction = {
        snackbar(ctx.getString(R.string.copy_success))
        val clipboardService = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboardService != null) {
            clipboardService.primaryClip = ClipData.newPlainText(null, text)

        } else {
            snackbar(ctx.getString(R.string.copy_failed))
        }
    }, failedAction = {

    })
}
