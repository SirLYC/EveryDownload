package com.lyc.everydownload.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.set
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.google.android.material.appbar.AppBarLayout
import com.lyc.everydownload.Async
import com.lyc.everydownload.BuildConfig
import com.lyc.everydownload.R
import com.lyc.everydownload.update.Progress
import com.lyc.everydownload.update.UpdateCheckListener
import com.lyc.everydownload.update.UpdateInfo
import com.lyc.everydownload.update.UpdateUtil
import com.lyc.everydownload.util.logE
import com.lyc.everydownload.util.snackbar
import com.lyc.everydownload.util.toDateString
import com.lyc.everydownload.util.toast
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.android.synthetic.main.layout_about_content.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.abs

/**
 * @author liuyuchuan
 * @date 2019-07-03
 * @email kevinliu.sir@qq.com
 */
class AboutActivity : AppCompatActivity(), UpdateCheckListener {

    override fun onGetUpdateInfo(info: UpdateInfo) {
        Async.instantMain.execute {
            if (info.code <= BuildConfig.VERSION_CODE) {
                toast("当前已经是最新版本")
            } else {
                AlertDialog.Builder(this)
                        .setTitle("新版本")
                        .setMessage("版本号：${info.name}.${info.code}\n" +
                                "发布时间: ${Date(info.time).toDateString()}\n" +
                                "更新说明:\n" +
                                info.des)
                        .setPositiveButton(R.string.update) { _, _ ->
                            UpdateUtil.downloadAndInstallUpdate((externalCacheDir
                                    ?: cacheDir).canonicalPath, info)
                        }
                        .setNegativeButton(R.string.not_update_now, null)
                        .show()
            }
        }
    }

    private fun setProgress(isChecking: Boolean, isDownloading: Boolean, progress: Progress?) {
        if (!isDownloading && !isChecking) {
            progress_bar.visibility = View.GONE
        } else {
            progress_bar.visibility = View.VISIBLE
            progress_bar.isIndeterminate = progress == null || isChecking == true
            progress?.run {
                if (progress.total < 1) {
                    progress_bar.isIndeterminate = true
                } else {
                    progress_bar.isIndeterminate = false
                    progress_bar.progress = ((progress.cur * 100.0 / progress.total).toInt())
                }
            }
        }
    }

    private fun setUpdateText(isChecking: Boolean, isDownloading: Boolean) {
        when {
            isChecking -> {
                tv_update.text = "正在检查更新"
                tv_update.isEnabled = false
            }
            isDownloading -> {
                tv_update.text = ("正在下载更新")
                tv_update.isEnabled = false
            }
            else -> {
                val updateSpb = SpannableStringBuilder(getString(R.string.click_update))
                updateSpb[0, updateSpb.length] = object : URLSpan("") {
                    override fun onClick(widget: View) {
                        UpdateUtil.checkUpdate(WeakReference(this@AboutActivity))
                    }
                }
                tv_update.isEnabled = true
                tv_update.text = updateSpb
                tv_update.movementMethod = LinkMovementMethod.getInstance()
            }
        }
        setProgress(isChecking, isDownloading, UpdateUtil.progressLiveData.value)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
        }

        UpdateUtil.progressLiveData.observe(this, Observer {
            setProgress(UpdateUtil.isChecking.value!!, UpdateUtil.isDownloading.value!!, it)
        })

        UpdateUtil.isChecking.observe(this, Observer {
            setUpdateText(it, UpdateUtil.isDownloading.value!!)
        })

        UpdateUtil.isDownloading.observe(this, Observer {
            setUpdateText(UpdateUtil.isChecking.value!!, it)
        })

        UpdateUtil.errorEvent.observeForever {
            tv_update.snackbar(it)
        }

        val title = toolbar.title
        toolbar.title = null

        app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            appBarLayout.post {
                // 1: collapsed 0: expanded
                val scale = abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange
                if (scale == 1f) {
                    toolbar.title = title
                    fab_email.isVisible = false
                } else {
                    toolbar.title = null
                    fab_email.isVisible = true
                    fab_email.alpha = 1 - scale
                }
            }
        })

        fab_email.setOnClickListener {
            val data = Intent(Intent.ACTION_SENDTO)
            data.data = Uri.parse("mailto:kevinliu.sir@qq.com")
            data.putExtra(Intent.EXTRA_SUBJECT, "EveryDownload-<标题>")
            try {
                startActivity(data)
            } catch (e: Exception) {
                logE("startActivity", e)
                fab_email.snackbar(getString(R.string.cannot_open_email))
            }
        }

        tv_version_content.text = getString(R.string.version_content_format).format(BuildConfig.VERSION_NAME)
        val relateLinkText = arrayOf(getString(R.string.app_project_link), getString(R.string.downloader_project_link), getString(R.string.apk_download_link), getString(R.string.my_blog))
        val urls = arrayOf("https://github.com/SirLYC/EveryDownload", "https://github.com/SirLYC/YC-Downloader", "${BuildConfig.RAW_URL}${BuildConfig.APK_NAME}", "https://juejin.im/user/592e23d3ac502e006c9afdd7/posts")
        val relateLinkSpb = SpannableStringBuilder(relateLinkText.joinToString("\n\n"))
        var start = 0
        relateLinkText.forEachIndexed { index, s ->
            relateLinkSpb[start, start + s.length] = URLSpan(urls[index])
            start += 2 + s.length
        }
        relate_link_content.text = relateLinkSpb
        relate_link_content.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.share -> {
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_TEXT, "Hi，这里有一个看起来不错的开源下载器～\nApk下载链接: ${BuildConfig.RAW_URL}${BuildConfig.APK_NAME}\n项目链接: https://github.com/SirLYC/EveryDownload")
                intent.type = "text/*"
                try {
                    startActivity(Intent.createChooser(intent, getString(R.string.share_to)))
                } catch (e: Exception) {
                    logE("startActivity", e)
                    fab_email.snackbar(getString(R.string.share_failed))
                }
                true
            }
            else -> {
                false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.about, menu)
        return true
    }


}
