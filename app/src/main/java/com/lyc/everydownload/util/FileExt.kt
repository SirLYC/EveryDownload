package com.lyc.everydownload.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.annotation.DrawableRes
import androidx.core.content.FileProvider
import com.lyc.everydownload.R
import com.lyc.everydownload.file.FileExploreActivity
import java.io.File

/**
 * @author liuyuchuan
 * @date 2019-06-20
 * @email kevinliu.sir@qq.com
 */

const val FILE_TYPE_DIR = 0
const val FILE_TYPE_APK = 1
const val FILE_TYPE_IMAGE = 2
const val FILE_TYPE_AUDIO = 3
const val FILE_TYPE_VIDEO = 4
const val FILE_TYPE_TXT = 5
const val FILE_TYPE_ARCHIVE = 6
const val FILE_TYPE_OTHER = 7
const val FILE_TYPE_UNKNOWN = -1

fun typeToIcon(type: Int) = when (type) {
    FILE_TYPE_DIR -> R.drawable.ic_folder_open_primary_24dp
    FILE_TYPE_APK -> R.drawable.ic_android_primary_24dp
    FILE_TYPE_IMAGE -> R.drawable.ic_image_primary_24dp
    FILE_TYPE_AUDIO -> R.drawable.ic_library_music_primary_24dp
    FILE_TYPE_VIDEO -> R.drawable.ic_videocam_primary_24dp
    FILE_TYPE_TXT -> R.drawable.ic_description_primary_24dp
    FILE_TYPE_ARCHIVE -> R.drawable.ic_archive_primary_24dp
    else -> R.drawable.ic_insert_drive_file_primary_24dp
}

@DrawableRes
fun File.getIcon() = typeToIcon(getType())

fun File.getType(): Int {
    if (isDirectory) return FILE_TYPE_DIR

    if (!isFile) return FILE_TYPE_UNKNOWN

    return when (extension.toLowerCase()) {
        "apk" -> FILE_TYPE_APK
        "mp3", "" -> FILE_TYPE_AUDIO
        "jpg", "jpeg", "png", "gif" -> FILE_TYPE_IMAGE
        "avi", "rmvb", "mp4", "3gp", "asf", "m4u", "m4v", "mov", "mpe", "mpeg", "mpeg4", "mpga" -> FILE_TYPE_VIDEO
        "txt", "java", "c", "cpp", "doc", "docx", "ppt", "pptx", "log", "m", "html", "css", "js", "ts", "dart", "py" -> FILE_TYPE_TXT
        "zip", "gz", "tar", "rar" -> FILE_TYPE_ARCHIVE
        else -> FILE_TYPE_OTHER
    }
}

fun String.getFileType(): Int {
    return when (substringAfterLast('.', "").toLowerCase()) {
        "apk" -> FILE_TYPE_APK
        "mp3", "" -> FILE_TYPE_AUDIO
        "jpg", "jpeg", "png", "gif" -> FILE_TYPE_IMAGE
        "avi", "rmvb", "mp4", "3gp", "asf", "m4u", "m4v", "mov", "mpe", "mpeg", "mpeg4", "mpga" -> FILE_TYPE_VIDEO
        "txt", "java", "c", "cpp", "doc", "docx", "ppt", "pptx", "log", "m", "html", "css", "js", "ts", "dart", "py" -> FILE_TYPE_TXT
        "zip", "gz", "tar", "rar" -> FILE_TYPE_ARCHIVE
        else -> FILE_TYPE_OTHER
    }
}

fun String.getFileIcon() = typeToIcon(getFileType())

inline fun Context.openFile(
        file: File,
        fileNotExistAction: () -> Unit = {},
        openFailAction: () -> Unit = {}
) {
    if (!file.exists()) {
        fileNotExistAction()
        return
    }

    val intent = Intent(Intent.ACTION_VIEW)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    val uri = if (Build.VERSION.SDK_INT >= 24) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        FileProvider.getUriForFile(this, "com.lyc.everydownload.fileprovider", file)
    } else {
        Uri.fromFile(file)
    }

    val type = (MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            ?: "*/*")
    logD("openFile, uri = $uri, type = $type")
    intent.setDataAndType(uri, type)

    try {
        startActivity(intent)
    } catch (e: Exception) {
        openFailAction()
        logE("openFile", e)
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

inline fun Context.shareFile(
        file: File,
        fileNotExistAction: () -> Unit = {},
        shareFailAction: () -> Unit = {}
) {
    if (!file.exists()) {
        fileNotExistAction()
        return
    }

    val intent = Intent(Intent.ACTION_SEND)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    val uri = if (Build.VERSION.SDK_INT >= 24) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        FileProvider.getUriForFile(this, "com.lyc.everydownload.fileprovider", file)
    } else {
        Uri.fromFile(file)
    }

    intent.putExtra(Intent.EXTRA_STREAM, uri.toString())

    val type = (MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            ?: "*/*")
    logD("openFile, uri = $uri, type = $type")
    intent.setDataAndType(uri, type)

    try {
        startActivity(Intent.createChooser(intent, getString(R.string.share_to)))
    } catch (e: Exception) {
        shareFailAction()
        logE("openFile", e)
    }
}
