package com.lyc.everydownload

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.lyc.downloader.DownloadTask.*
import com.lyc.everydownload.util.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

/**
 * Created by Liu Yuchuan on 2019/4/22.
 */
class MainActivity : AppCompatActivity(), DownloadItemViewBinder.OnItemButtonClickListener {

    private lateinit var mainViewModel: MainViewModel
    private lateinit var adapter: ReactiveAdapter
    private val itemCallback = object : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is DownloadItem && newItem is DownloadItem) {
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
            register(String::class, GroupHeaderItemViewBinder())
            register(DownloadItem::class, DownloadItemViewBinder(this@MainActivity))
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
        return if (item.itemId == R.id.add) {
            StartDownloadDialog().show(supportFragmentManager, "startDownload")
            true
        } else super.onOptionsItemSelected(item)

    }

    private fun tryToOpen(file: File) {
        doWithRWPermission({
            openFile(file)
        }, {
            tryToOpen(file)
        })
    }

    override fun openItemFile(item: DownloadItem) {
        val name = item.filename
        tryToOpen(File(item.path, name))
    }

    override fun openItemFileNotExist(item: DownloadItem) {
        toast(getString(R.string.download_file_not_exists))
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
        menu.add(0, 1, 0, "删除")
        when (state) {
            PAUSED, FINISH -> menu.add(0, 2, 0, "重新下载")
        }
        if (state != FINISH && state != CANCELED) {
            menu.add(0, 3, 0, "取消")
        }
        menu.add(0, 4, 0, "打开文件夹")
        menu.add(0, 5, 0, "选择")
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                1 -> showDeleteAssureDialog(item.id)
                2 -> showReDownloadDialog(item.id)
                3 -> mainViewModel.cancel(item.id)
                4 -> tryToOpen(File(item.path))
                5 -> {
                    // TODO 2019-06-12 @liuyuchuan: select
                }
            }
            true
        }
        popupMenu.show()
        return true
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
