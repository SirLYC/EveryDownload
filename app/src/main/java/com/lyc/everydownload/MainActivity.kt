package com.lyc.everydownload

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckBox
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.setPadding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.lyc.downloader.DownloadTask.FINISH
import com.lyc.downloader.DownloadTask.PAUSED
import com.lyc.downloader.YCDownloader
import com.lyc.everydownload.preference.AppPreference
import com.lyc.everydownload.preference.PreferenceActivity
import com.lyc.everydownload.util.*
import com.lyc.everydownload.util.rv.ReactiveAdapter
import com.lyc.everydownload.util.rv.VerticalItemDecoration
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

/**
 * Created by Liu Yuchuan on 2019/4/22.
 */
class MainActivity : AppCompatActivity(), DownloadItemViewBinder.OnItemButtonClickListener, OnItemClickListener<DownloadGroupHeader> {
    override fun onItemClick(v: View, value: DownloadGroupHeader, index: Int) {
        if (value.id == ActiveDownloadListHolder.ID_DOWNLOADING) {
            ActiveDownloadListHolder.expandDownloading = !value.expand
        } else {
            ActiveDownloadListHolder.expandFinished = !value.expand
        }
    }

    private lateinit var mainViewModel: MainViewModel
    private lateinit var adapter: ReactiveAdapter
    private val itemCallback = object : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is DownloadItem && newItem is DownloadItem) {
                oldItem.id == newItem.id
            } else if (oldItem is DownloadGroupHeader && newItem is DownloadGroupHeader) {
                oldItem.id == newItem.id
            } else if (oldItem is EmptyListItem && newItem is EmptyListItem) {
                oldItem.id == newItem.id
            } else {
                oldItem == newItem
            }
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is DownloadItem && newItem is DownloadItem) {
                (oldItem.downloadState == newItem.downloadState &&
                        oldItem.bps == newItem.bps
                        && oldItem.downloadedSize == newItem.downloadedSize)
            } else if (oldItem is DownloadGroupHeader && newItem is DownloadGroupHeader) {
                oldItem.count == newItem.count && oldItem.expand == newItem.expand
            } else if (oldItem is EmptyListItem && newItem is EmptyListItem) {
                true
            } else {
                return oldItem == newItem
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        rv.layoutManager = LinearLayoutManager(this)
        rv.addItemDecoration(VerticalItemDecoration((resources.displayMetrics.density * 16).toInt()))
        adapter = ReactiveAdapter(ActiveDownloadListHolder.itemList).apply {
            register(DownloadGroupHeader::class, DownloadGroupHeaderItemViewBinder(this@MainActivity))
            register(DownloadItem::class, DownloadItemViewBinder(this@MainActivity))
            register(EmptyListItem::class, EmptyItemViewBinder())
            observe(this@MainActivity)
            rv.adapter = this
        }

        ActiveDownloadListHolder.itemListLivaData.observe(this, Observer {
            adapter.replaceList(it, itemCallback, true)
        })

        val itemAnimator = rv.itemAnimator
        if (itemAnimator != null) {
            itemAnimator.changeDuration = 0
            val animator = itemAnimator as SimpleItemAnimator?
            animator!!.supportsChangeAnimations = false
        }

        ActiveDownloadListHolder.refreshLiveDate.observe(this, Observer {
            refresh.isRefreshing = it
        })

        refresh.setOnRefreshListener {
            ActiveDownloadListHolder.refreshList()
        }

        mainViewModel.failLiveData.observe(this, Observer {
            rv.snackbar(it)
        })
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when {
            item.itemId == R.id.add -> {
                StartDownloadDialog().show(supportFragmentManager, "startDownload")
                true
            }
            item.itemId == R.id.preference -> {
                startActivity(Intent(this, PreferenceActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    private fun tryToOpen(file: File) {
        doWithRWPermission({
            openFile(file, fileNotExistAction = {
                rv.snackbar(getString(R.string.download_file_not_exists))
            }, openFailAction = {
                rv.snackbar(getString(R.string.open_download_file))
            })
        }, {
            tryToOpen(file)
        })
    }

    private fun tryToOpenFolder(file: File, targetFilename: String) {
        doWithRWPermission({
            openFileInner(file, targetFile = targetFilename) {
                rv.snackbar(getString(R.string.dir_not_exist))
            }
        }, {
            tryToOpenFolder(file, targetFilename)
        })
    }

    override fun openItemFile(item: DownloadItem) {
        val name = item.filename
        tryToOpen(File(item.path, name))
    }

    override fun openItemFolder(item: DownloadItem) {
        tryToOpenFolder(File(item.path), item.filename)
    }

    override fun openItemFileNotExist(item: DownloadItem) {
        rv.snackbar(getString(R.string.download_file_not_exists))
    }

    override fun pauseItem(item: DownloadItem) {
        mainViewModel.pause(item.id)
    }

    override fun startItem(item: DownloadItem) {
        mainViewModel.start(item.id)
    }

    override fun onItemLongClicked(item: DownloadItem, view: View): Boolean {
        val state = item.downloadState
        val popupMenu = PopupMenu(this, view)
        val menu = popupMenu.menu

        if (state == FINISH) {
            menu.add(0, 1, 0, "分享")
        }
        menu.add(0, 2, 0, "删除")
        when (state) {
            PAUSED, FINISH -> menu.add(0, 3, 0, "重新下载")
        }
        menu.add(0, 4, 0, "复制下载链接")
        menu.add(0, 5, 0, "复制存放路径")
        menu.add(0, 6, 0, "打开文件夹")
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                1 -> shareFile(File(item.path, item.filename), fileNotExistAction = {
                    rv.snackbar(getString(R.string.dir_not_exist))
                }, shareFailAction = {
                    rv.snackbar(getString(R.string.share_failed))
                })
                2 -> showDeleteAssureDialog(item.id)
                3 -> showReDownloadDialog(item.id)
                4 -> rv.copyPlainWithSnackBarTip(item.url)
                5 -> rv.copyPlainWithSnackBarTip(item.path)
                6 -> tryToOpenFolder(File(item.path), item.filename)
            }
            true
        }
        popupMenu.show()
        return true
    }

    override fun onBackPressed() {
        logD("back:${AppPreference.backgroundDownload}")
        if (!AppPreference.backgroundDownload && ActiveDownloadListHolder.hasAnyDownloadingTask()) {
            val frameLayout = FrameLayout(this)
            frameLayout.setPadding((resources.displayMetrics.density * 32).toInt())
            val checkBox = CheckBox(this)
            checkBox.text = getString(R.string.continue_downloading)
            frameLayout.addView(checkBox, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            AlertDialog.Builder(this)
                    .setView(frameLayout)
                    .setMessage(getString(R.string.exit_prompt))
                    .setPositiveButton(R.string.exit) { _, _ ->
                        if (checkBox.isChecked) {
                            AppPreference.backgroundDownload = true
                        } else {
                            YCDownloader.pauseAll()
                        }
                        super.onBackPressed()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        } else {
            super.onBackPressed()
        }
    }

    private fun showDeleteAssureDialog(id: Long) {
        val dialog = DeleteDownloadDialog()
        dialog.arguments = Bundle().apply {
            putLong("id", id)
        }
        dialog.show(supportFragmentManager, "delete")
    }

    private fun showReDownloadDialog(id: Long) {
        val dialog = RestartDownloadDialog()
        dialog.arguments = Bundle().apply {
            putLong("id", id)
        }
        dialog.show(supportFragmentManager, "restart")
    }

    override fun onDestroy() {
        adapter.removeItemCallback(itemCallback)
        super.onDestroy()
    }
}
