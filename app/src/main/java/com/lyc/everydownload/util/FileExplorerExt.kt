package com.lyc.everydownload.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.lyc.everydownload.R
import java.io.File

/**
 * @author liuyuchuan
 * @date 2019-06-12
 * @email kevinliu.sir@qq.com
 */
fun Context.openFile(file: File) {
    if (!file.exists()) {
        toast(R.string.download_file_not_exists)
        return
    }
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    intent.setDataAndType(Uri.fromFile(file), "*/*")
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
        startActivity(intent)
    } else {
        toast(R.string.no_activity_to_open_file)
    }
}
