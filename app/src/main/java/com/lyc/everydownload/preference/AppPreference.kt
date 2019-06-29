package com.lyc.everydownload.preference

import android.app.ActivityManager
import android.content.*
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import androidx.preference.PreferenceManager
import com.lyc.downloader.Configuration
import com.lyc.downloader.Configuration.DEFAULT_AVOID_FRAME_DROP
import com.lyc.downloader.Configuration.DEFAULT_MAX_RUNNING_TASK
import com.lyc.downloader.YCDownloader
import com.lyc.everydownload.R
import com.lyc.everydownload.util.logD


/**
 * @author liuyuchuan
 * @date 2019-06-29
 * @email kevinliu.sir@qq.com
 */
object AppPreference : SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var appContext: Context
    private lateinit var preferences: SharedPreferences
    var maxRunningTask = DEFAULT_MAX_RUNNING_TASK
    var speedLimit = 0L
    var allowOperatorDownload = false
    var avoidFrameDrop = DEFAULT_AVOID_FRAME_DROP
    var backgroundDownload = true
    var currentNetworkState = 0
    var useLastDownloadDir = true
    var defaultDownloadDir: String? = null
    var lastDownloadDir: String? = null
        set(value) {
            if (field != value) {
                field = value
                preferences.edit()
                        .putString("key_last_download", lastDownloadDir)
                        .apply()
            }
        }

    private const val FLAG_CONNECTED = 1
    private const val FLAG_WIFI = 1 shl 1


    fun setup(context: Context) {
        appContext = context.applicationContext
        preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        preferences.registerOnSharedPreferenceChangeListener(this)
        maxRunningTask = preferences.getParsedInt(appContext.getString(R.string.key_max_running_tasks), maxRunningTask)
        speedLimit = preferences.getParsedLong(appContext.getString(R.string.key_speed_limit), speedLimit)
        allowOperatorDownload = preferences.getBoolean(appContext.getString(R.string.key_allow_operator_network), allowOperatorDownload)
        avoidFrameDrop = preferences.getBoolean(appContext.getString(R.string.key_avoid_frame_drop), avoidFrameDrop)
        useLastDownloadDir = preferences.getBoolean(appContext.getString(R.string.key_use_last_download), useLastDownloadDir)
        lastDownloadDir = preferences.getString("key_last_download", null)
        defaultDownloadDir = preferences.getString(appContext.getString(R.string.key_default_download_dir), Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).canonicalPath)
        val config = Configuration.Builder()
                .setMaxRunningTask(maxRunningTask)
                .setSpeedLimit(speedLimit)
                .setAllowDownload(false)
                .build()
        YCDownloader.install(appContext, config)
        context.registerReceiver(NetworkDetectBroadcastReceiver(), IntentFilter().apply {
            addAction("android.net.conn.CONNECTIVITY_CHANGE")
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        })
        if (getProcessName() == appContext.packageName) {
            updateAllowDownload(appContext)
            if (Build.VERSION.SDK_INT >= 23) {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager?
                cm?.registerNetworkCallback(
                        NetworkRequest.Builder().addCapability(NET_CAPABILITY_VALIDATED).addTransportType(TRANSPORT_WIFI).build(),
                        object : ConnectivityManager.NetworkCallback() {
                            override fun onCapabilitiesChanged(network: Network?, networkCapabilities: NetworkCapabilities?) {
                                logD("onCap change!")
                                updateAllowDownload(appContext)
                            }
                        })
            }
        }
    }

    private fun getProcessName(): String? {
        val pid = android.os.Process.myPid()
        val activityService = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var processName: String? = null
        for (runningAppProcess in activityService.runningAppProcesses) {
            if (runningAppProcess.pid == pid) {
                processName = runningAppProcess.processName
            }
        }
        return processName
    }

    private fun SharedPreferences.getParsedInt(key: String, defaultValue: Int) = getString(key, defaultValue.toString())?.toIntOrNull()
            ?: defaultValue

    private fun SharedPreferences.getParsedLong(key: String, defaultValue: Long) = getString(key, defaultValue.toString())?.toLongOrNull()
            ?: defaultValue


    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        when (key) {
            appContext.getString(R.string.key_max_running_tasks) -> {
                maxRunningTask = preferences.getParsedInt(key, DEFAULT_MAX_RUNNING_TASK)
                YCDownloader.setMaxRunningTask(maxRunningTask)

            }

            appContext.getString(R.string.key_speed_limit) -> {
                this.speedLimit = preferences.getParsedLong(key, 0L)
                YCDownloader.setSpeedLimit(speedLimit * 1024)
            }

            appContext.getString(R.string.key_allow_operator_network) -> {
                this.allowOperatorDownload = preferences.getBoolean(key, false)
                updateAllowDownload(appContext)
            }

            appContext.getString(R.string.key_download_background) -> {
                this.backgroundDownload = preferences.getBoolean(key, true)
            }

            appContext.getString(R.string.key_avoid_frame_drop) -> {
                avoidFrameDrop = preferences.getBoolean(key, DEFAULT_AVOID_FRAME_DROP)
                YCDownloader.setAvoidFrameDrop(avoidFrameDrop)
            }

            appContext.getString(R.string.key_default_download_dir) -> {
                this.defaultDownloadDir = preferences.getString(key, null)
            }

            appContext.getString(R.string.key_use_last_download) -> {
                this.useLastDownloadDir = preferences.getBoolean(key, true)
            }
        }
    }

    private class NetworkDetectBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            logD("onReceive")
            updateAllowDownload(context.applicationContext)
        }
    }

    val isAllowOperatorNetwork
        get() = allowOperatorDownload

    val isConnectedToNetwork
        get() = (currentNetworkState and FLAG_CONNECTED) != 0

    val isConnectedToWifi
        get() = (currentNetworkState and FLAG_WIFI) != 0

    fun updateAllowDownload(context: Context) {
        if (updateNetworkState(context)) {
            if (allowOperatorDownload) {
                YCDownloader.setAllowDownload(isConnectedToNetwork)
            } else {
                YCDownloader.setAllowDownload(isConnectedToWifi)
            }
        }
    }

    /**
     * return if state change
     */
    private fun updateNetworkState(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager?

        if (cm != null) {

            var flag = 0
            if (Build.VERSION.SDK_INT < 23) {
                cm.activeNetworkInfo?.let { ni ->
                    if (ni.isConnected) {
                        flag = flag or FLAG_CONNECTED
                        @Suppress("DEPRECATION")
                        if (ni.type == ConnectivityManager.TYPE_WIFI) {
                            flag = flag or FLAG_WIFI
                        }
                    }
                }
            } else {
                cm.activeNetwork?.let { n ->
                    val nc = cm.getNetworkCapabilities(n)
                    if (nc.hasCapability(NET_CAPABILITY_VALIDATED)) {
                        flag = flag or FLAG_CONNECTED
                        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            flag = flag or FLAG_WIFI
                        }
                    }
                }
            }

            if (currentNetworkState != flag) {
                currentNetworkState = flag
                return true
            }

            return false
        }

        currentNetworkState = 0
        return true
    }
}
