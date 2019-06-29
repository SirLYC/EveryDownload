package com.lyc.everydownload

import android.app.Application
import android.content.Context
import com.lyc.everydownload.preference.AppPreference
import com.lyc.everydownload.util.ActiveDownloadListHolder
import com.lyc.everydownload.util.BlockDetectByPrinter

/**
 * Created by Liu Yuchuan on 2019/5/18.
 */
class App : Application() {
    companion object {
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        // multi process
        AppPreference.setup(this)
        ActiveDownloadListHolder.setup()
        // single process
        //        YCDownloader.install(this, false);
        // or
        //        YCDownloader.install(this);
        if (BuildConfig.DEBUG) {
            BlockDetectByPrinter.start()
        }
    }
}
