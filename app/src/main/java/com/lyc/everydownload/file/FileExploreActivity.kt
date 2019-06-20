package com.lyc.everydownload.file

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.lyc.everydownload.R
import com.lyc.everydownload.util.ReactiveAdapter
import com.lyc.everydownload.util.closeAllAnimation
import com.lyc.everydownload.util.logW
import com.lyc.everydownload.util.toast
import kotlinx.android.synthetic.main.activity_file_explore.*
import java.io.File

/**
 * @author liuyuchuan
 * @date 2019-06-19
 * @email kevinliu.sir@qq.com
 */
class FileExploreActivity : AppCompatActivity(), ReactiveAdapter.ReplaceCommitAction {
    override fun onCommitReplace() {
        fileExploreViewModel.lastViewDir?.let {
            val index = fileExploreViewModel.itemList.indexOf(it)
            if (index >= 0 && index < fileExploreViewModel.itemList.size) {
                rv.scrollToPosition(index)
            }
        }
    }

    companion object {
        const val KEY_DIR = "KEY_DIR"
        const val KEY_PATH = "PATH"
    }

    private lateinit var fileExploreViewModel: FileExploreViewModel
    private var isDir = false
    private var currentPath: String = ""
    private lateinit var adapter: ReactiveAdapter
    private val callback = object : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return (oldItem as? File)?.canonicalPath == (newItem as? File)?.canonicalPath
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            val oldFile = oldItem as File
            val newFile = newItem as File

            return oldFile.lastModified() == newFile.lastModified() && oldFile.length() == newFile.length()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_explore)
        setSupportActionBar(toolbar)

        fileExploreViewModel = ViewModelProviders.of(this).get(FileExploreViewModel::class.java)
        val rootFile = Environment.getExternalStorageDirectory() ?: File("/")

        var getArgs = false
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_DIR) && savedInstanceState.containsKey(KEY_PATH)) {
                getArgs = true
                isDir = savedInstanceState.getBoolean(KEY_DIR)
                currentPath = savedInstanceState.getString(KEY_PATH) ?: ""
            }
        }

        if (!getArgs) {
            isDir = intent.getBooleanExtra(KEY_DIR, false)
            currentPath = intent.getStringExtra(KEY_PATH) ?: ""
        }


        adapter = ReactiveAdapter(fileExploreViewModel.itemList).apply {
            register(File::class, FileItemViewBinder { file, index ->
                if (!file.exists()) {
                    fileExploreViewModel.itemList.let {
                        if (index >= 0 && index < it.size && it[index] == file) {
                            it.remove(index)
                        }
                    }
                } else if (file.isDirectory) {
                    fileExploreViewModel.chDir(file)
                } else {
                    // TODO 2019-06-20
                    logW("TODO: Click file action")
                }
            })
            observe(this@FileExploreActivity)
            rv.adapter = this
        }


        rv.layoutManager = LinearLayoutManager(this)
        fileExploreViewModel.dirLiveData.observe(this, Observer {
            tv_path.text = it.canonicalPath
        })

        rv.closeAllAnimation()

        fileExploreViewModel.newListEvent.observe(this, Observer {
            if (it.isEmpty()) {

                adapter.list.clear()
            } else {
                adapter.replaceList(it, callback, true)
            }

            val gone = it.isEmpty()
            rv.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (gone) {
                        empty_view.visibility = VISIBLE
                        rv.visibility = GONE
                    } else {
                        empty_view.visibility = GONE
                        rv.visibility = VISIBLE
                    }
                    fileExploreViewModel.lastViewDir?.let { file ->
                        val index = fileExploreViewModel.itemList.indexOf(file)
                        if (index >= 0 && index < fileExploreViewModel.itemList.size) {
                            rv.scrollToPosition(index)
                            rv.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                }
            })
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
                } ?: kotlin.run {
                    toast("文件夹已失效")
                    fileExploreViewModel.chDir(fileExploreViewModel.root)
                }
            }
        } else {
            supportActionBar?.title = getString(R.string.explore_file)
            bt_cancel.text = getString(R.string.back)
            button_bar.visibility = GONE
        }

        if (fileExploreViewModel.itemList.isEmpty()) {
            rv.visibility = GONE
            empty_view.visibility = VISIBLE
        } else {
            rv.visibility = VISIBLE
            empty_view.visibility = GONE
        }

        bt_cancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        fileExploreViewModel.setup(rootFile, currentPath)

    }

    override fun onBackPressed() {
        if (!fileExploreViewModel.isRoot()) {
            fileExploreViewModel.back()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_DIR, isDir)
        outState.putString(KEY_PATH, currentPath)
    }

    override fun onDestroy() {
        adapter.remove(callback)
        super.onDestroy()
    }
}
