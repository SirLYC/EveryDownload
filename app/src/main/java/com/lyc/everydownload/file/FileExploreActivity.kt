package com.lyc.everydownload.file

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.view.View.*
import android.view.ViewTreeObserver
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.lyc.everydownload.R
import com.lyc.everydownload.util.*
import com.lyc.everydownload.util.rv.ReactiveAdapter
import com.lyc.everydownload.util.rv.closeAllAnimation
import com.lyc.everydownload.widget.MultiStateView
import kotlinx.android.synthetic.main.activity_file_explore.*
import java.io.File

/**
 * @author liuyuchuan
 * @date 2019-06-19
 * @email kevinliu.sir@qq.com
 */
class FileExploreActivity : AppCompatActivity(), OnItemClickListener<File>, OnItemLongClickListener<File> {
    override fun onItemClick(v: View, value: File, index: Int) {
        if (!value.exists()) {
            fileExploreViewModel.itemList.let {
                if (index >= 0 && index < it.size && it[index] == value) {
                    it.remove(index)
                }
            }
        } else if (value.isDirectory) {
            fileExploreViewModel.chDir(value)
        } else {
            if (!isDir) {
                // TODO 2019-06-20
                logW("TODO: Click file action")
            }
        }
    }

    override fun onItemLongClick(v: View, value: File, index: Int): Boolean {
        val popupMenu = PopupMenu(this, v)
        popupMenu.menu.run {
            add(0, 0, 0, R.string.delete)
        }
        popupMenu.setOnMenuItemClickListener { menu ->
            when (menu.itemId) {
                0 -> {
                    AlertDialog.Builder(this)
                            .setMessage("确定删除\"%s\"吗？".format(value.name))
                            .setPositiveButton(R.string.ok) { _, _ ->
                                fileExploreViewModel.del(value)
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
        return true
    }

    companion object {
        const val KEY_DIR = "KEY_DIR"
        const val KEY_PATH = "KEY_PATH"
        const val KEY_TARGET_FILE = "KEY_TARGET_FILE"
    }

    private lateinit var fileExploreViewModel: FileExploreViewModel
    private var isDir = false
    private var currentPath: String = ""
    private var targetFile: File? = null
    private var jumpToTargetFile = false
    private lateinit var adapter: ReactiveAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_explore)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fileExploreViewModel = ViewModelProviders.of(this).get(FileExploreViewModel::class.java)
        val rootFile = Environment.getExternalStorageDirectory() ?: File("/")

        var getArgs = false
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_DIR) && savedInstanceState.containsKey(KEY_PATH)) {
                getArgs = true
                isDir = savedInstanceState.getBoolean(KEY_DIR)
                currentPath = savedInstanceState.getString(KEY_PATH) ?: ""
                savedInstanceState.getString(KEY_TARGET_FILE)?.let {
                    targetFile = File(it)
                }
            }
        }

        if (!getArgs) {
            isDir = intent.getBooleanExtra(KEY_DIR, false)
            currentPath = intent.getStringExtra(KEY_PATH) ?: ""
            intent.getStringExtra(KEY_TARGET_FILE)?.let {
                targetFile = File(currentPath, it)
            }
        }

        targetFile?.run {
            if (exists()) {
                jumpToTargetFile = false
            }
        }

        adapter = ReactiveAdapter(fileExploreViewModel.itemList).apply {
            register(File::class, FileItemViewBinder(this@FileExploreActivity, this@FileExploreActivity))
            observe(this@FileExploreActivity)
            rv.adapter = this
        }


        rv.layoutManager = LinearLayoutManager(this)
        fileExploreViewModel.dirLiveData.observe(this, Observer {
            tv_path.text = it.canonicalPath
        })

        rv.closeAllAnimation()

        fileExploreViewModel.fileListLiveData.observe(this, Observer {
            adapter.list = it
            adapter.notifyDataSetChanged()
            val gone = it.isEmpty()
            if (!gone) {
                msv.showContent()
                rv.visibility = INVISIBLE
                var delayVis = false
                val targetFile = targetFile
                val file = if (!jumpToTargetFile && targetFile != null) {
                    jumpToTargetFile = true
                    targetFile
                } else {
                    fileExploreViewModel.lastViewDir()
                }

                file?.let { jumpToFile ->
                    val index = fileExploreViewModel.itemList.indexOf(jumpToFile)
                    if (index >= 0 && index < fileExploreViewModel.itemList.size) {
                        delayVis = true
                        rv.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                rv.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                var post = false
                                if (index >= 0 && index < fileExploreViewModel.itemList.size) {
                                    rv.scrollToPosition(index)
                                    rv.stopScroll()
                                    post = true
                                    rv.post {
                                        rv.visibility = VISIBLE
                                    }
                                }
                                if (!post) {
                                    rv.visibility = VISIBLE
                                }
                            }
                        })
                    }
                }

                if (!delayVis) {
                    rv.visibility = VISIBLE
                }
            } else {
                msv.showState(MultiStateView.STATE_EMPTY)
            }
        })


        if (isDir) {
            supportActionBar?.title = getString(R.string.choose_dir)
            bt_choose.visibility = VISIBLE

            bt_choose.setOnClickListener {
                fileExploreViewModel.currentDir?.also {
                    setResult(Activity.RESULT_OK, Intent().apply {
                        data = Uri.fromFile(it)
                    })
                    finish()
                } ?: this.run {
                    toast("文件夹已失效")
                    fileExploreViewModel.chDir(fileExploreViewModel.root)
                }
            }
        } else {
            supportActionBar?.title = getString(R.string.explore_file)
            bt_cancel.text = getString(R.string.back)
            button_bar.visibility = GONE
        }

        bt_create_mkdir.setOnClickListener {
            showDialog<MkdirDialog>()
        }

        if (fileExploreViewModel.itemList.isEmpty()) {
            msv.showState(MultiStateView.STATE_EMPTY)
        } else {
            msv.showContent()
        }

        bt_cancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        refresh.setOnRefreshListener {
            fileExploreViewModel.refresh()
            refresh.isRefreshing = false
        }

        fileExploreViewModel.successMsg.observe(this, Observer {
            toast(it)
        })

        fileExploreViewModel.errorMsg.observe(this, Observer {
            rv.snackbar(it)
        })

        fileExploreViewModel.setup(rootFile, currentPath, isDir)
    }

    override fun onBackPressed() {
        if (!fileExploreViewModel.isRoot()) {
            fileExploreViewModel.back()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_DIR, isDir)
        outState.putString(KEY_PATH, currentPath)
        outState.putString(KEY_TARGET_FILE, targetFile?.name)
    }
}
