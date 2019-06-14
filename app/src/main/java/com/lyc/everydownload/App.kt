package com.lyc.everydownload

import android.app.Application
import com.lyc.downloader.YCDownloader

/**
 * Created by Liu Yuchuan on 2019/5/18.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // multi process
        YCDownloader.install(this, true)
        // single process
        //        YCDownloader.install(this, false);
        // or
        //        YCDownloader.install(this);
    }
}
