package com.lyc.everydownload.util

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.lyc.everydownload.file.FileExploreActivity
import java.io.File

/**
 * @author liuyuchuan
 * @date 2019-06-12
 * @email kevinliu.sir@qq.com
 */
inline fun Context.openFile(
        file: File,
        fileNotExistAction: () -> Unit = {},
        openFailAction: () -> Unit = {}
) {
    if (!file.exists()) {
        fileNotExistAction()
        return
    }

    logD("openFile, path = ${file.canonicalPath}")
    val intent = Intent(Intent.ACTION_GET_CONTENT)

    val uri = if (Build.VERSION.SDK_INT >= 24) {
        intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION)
        FileProvider.getUriForFile(this, "com.lyc.everydownload.fileprovider", file)
    } else {
        Uri.fromFile(file)
    }

    logD("openFile, uri = $uri")
    intent.setDataAndType(uri, "*/*")
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
        startActivity(intent)
    } else {
        openFailAction()
    }
}

inline fun Context.openFileInner(
        file: File,
        targetFile: String? = null,
        fileNotExistAction: () -> Unit = {}
) {
    if (!file.exists()) {
        fileNotExistAction()
        return
    }

    val intent = Intent(this, FileExploreActivity::class.java)
    intent.putExtra(FileExploreActivity.KEY_PATH, file.path)
    intent.putExtra(FileExploreActivity.KEY_TARGET_FILE, targetFile)

    startActivity(intent)
}

fun Fragment.chooseFilePath() {
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    intent.type = "*/*"
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    startActivityForResult(intent, 1)
}
