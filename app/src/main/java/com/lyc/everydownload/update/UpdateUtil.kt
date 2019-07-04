package com.lyc.everydownload.update

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import com.lyc.everydownload.Async
import com.lyc.everydownload.BuildConfig
import com.lyc.everydownload.util.*
import org.json.JSONObject
import java.io.*
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL

/**
 * @author liuyuchuan
 * @date 2019-07-04
 * @email kevinliu.sir@qq.com
 */
object UpdateUtil {
    val errorEvent = SingleLiveEvent<String>()
    val isChecking = MutableLiveData<Boolean>().apply {
        value = false
    }
    val isDownloading = MutableLiveData<Boolean>().apply {
        value = false
    }
    val progressLiveData = MutableLiveData<Progress>()

    val finishDownloadCall = SingleLiveEvent<File>()

    private var updateInfoLiveData = MutableLiveData<UpdateInfo>()

    @MainThread
    fun checkUpdate(listener: WeakReference<UpdateCheckListener>) {
        val checking = isChecking.value!!
        val downloading = isDownloading.value!!
        if (checking || downloading) {
            return
        }

        val info = updateInfoLiveData.value
        if (info != null) {
            listener.get()?.run {
                onGetUpdateInfo(info)
            }
            return
        }

        isChecking.value = true
        progressLiveData.value = null
        Async.cache.execute {
            val url = URL("${BuildConfig.RAW_URL}${BuildConfig.INFO_FILE_NAME}")
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connectTimeout = 8000
            urlConnection.readTimeout = 8000
            urlConnection.allowUserInteraction = true
            urlConnection.requestMethod = "GET"

            var reader: BufferedReader? = null
            try {
                val inputStream = url.openStream()
                reader = inputStream.bufferedReader()
                isChecking.postValue(false)
                processUpdateInfo(reader.readText(), listener)
            } catch (e: Exception) {
                logE("checkUpdate", e)
                errorEvent.postValue("检查更新失败[${e.message}]")
            } finally {
                urlConnection.closeQuietly()
                reader?.closeQuietly()
                isChecking.postValue(false)
            }
        }
    }

    private fun processUpdateInfo(infoResp: String, listener: WeakReference<UpdateCheckListener>) {
        val json = JSONObject(infoResp)
        val info = UpdateInfo(
                json.getInt("code"),
                json.getString("name"),
                json.getString("filename"),
                json.getString("url"),
                json.getLong("time"),
                json.getString("des"),
                json.getLong("size"),
                json.getString("md5")
        )
        updateInfoLiveData.postValue(info)
        listener.get()?.run {
            onGetUpdateInfo(info)
        }
    }

    fun downloadAndInstallUpdate(path: String, info: UpdateInfo) {
        val checking = isChecking.value!!
        val downloading = isDownloading.value!!
        if (checking || downloading) {
            return
        }

        isDownloading.value = true
        progressLiveData.value = null

        Async.cache.execute {
            try {
                val file = File(path, info.filename)
                if (file.exists() && file.length() == info.size && file.md5() == info.md5) {
                    finishDownloadCall.postValue(file)
                    return@execute
                }

                runIfDebug {
                    if (file.exists()) {
                        logW("Update apk exists: length = ${file.length()}, md5=${file.md5()}")
                    }
                }

                val url = URL(info.url)
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.connectTimeout = 8000
                urlConnection.readTimeout = 8000
                urlConnection.allowUserInteraction = true
                urlConnection.requestMethod = "GET"

                var inputStream: InputStream? = null
                var outputStream: OutputStream? = null
                var success = false
                try {
                    inputStream = url.openStream()
                    outputStream = FileOutputStream(file)
                    var read: Int
                    var download = 0L
                    val buffer = ByteArray(2048)
                    do {
                        read = inputStream.read(buffer)
                        if (read > 0) {
                            outputStream.write(buffer, 0, read)
                            download += read
                            progressLiveData.postValue(Progress(download, info.size))
                        }
                    } while (read >= 0)
                    success = true
                } catch (e: Exception) {
                    logE("downloadAndInstallUpdate", e)
                    errorEvent.postValue("下载更新失败[${e.message}]")
                    file.delete()
                } finally {
                    urlConnection.closeQuietly()
                    inputStream?.closeQuietly()
                    if (success) {
                        outputStream?.flushQuietly()
                        finishDownloadCall.postValue(file)
                    }
                    outputStream?.closeQuietly()
                }
            } finally {
                isDownloading.postValue(false)
            }
        }
    }
}
