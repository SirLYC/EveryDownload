package com.lyc.everydownload.file

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.SimpleItemAnimator
import com.lyc.everydownload.R
import com.lyc.everydownload.util.ReactiveAdapter
import com.lyc.everydownload.util.logW
import com.lyc.everydownload.util.toast
import kotlinx.android.synthetic.main.activity_file_explore.*
import java.io.File

/**
 * @author liuyuchuan
 * @date 2019-06-19
 * @email kevinliu.sir@qq.com
 */
class FileExploreActivity : AppCompatActivity(), ListUpdateCallback {
    override fun onChanged(position: Int, count: Int, payload: Any?) {}

    override fun onMoved(fromPosition: Int, toPosition: Int) {}

    override fun onInserted(position: Int, count: Int) {
        if (fileExploreViewModel.itemList.size - count == 0) {
            rv.visibility = VISIBLE
            empty_view.visibility = GONE
        }
    }

    override fun onRemoved(position: Int, count: Int) {
        if (fileExploreViewModel.itemList.isEmpty()) {
            rv.visibility = GONE
            empty_view.visibility = VISIBLE
        }
    }

    companion object {
        const val KEY_DIR = "KEY_DIR"
        const val KEY_PATH = "PATH"
    }

    private lateinit var fileExploreViewModel: FileExploreViewModel
    private var isDir = false
    private var currentPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_explore)
        setSupportActionBar(toolbar)

        fileExploreViewModel = ViewModelProviders.of(this).get(FileExploreViewModel::class.java)
        val rootFile = Environment.getExternalStorageDirectory() ?: File("/")
        fileExploreViewModel.setup(rootFile)

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

        File(currentPath).let {
            if (it.exists() && it.isDirectory) {
                fileExploreViewModel.chDir(it)
            } else {
                currentPath = rootFile.absolutePath
            }
        }


        ReactiveAdapter(fileExploreViewModel.itemList).apply {
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

        fileExploreViewModel.itemList.addCallback(this)

        rv.layoutManager = LinearLayoutManager(this)
        fileExploreViewModel.dirLiveData.observe(this, Observer {
            tv_path.text = it.canonicalPath
        })

        rv.itemAnimator?.addDuration = 0
        rv.itemAnimator?.changeDuration = 0
        (rv.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false


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
        fileExploreViewModel.itemList.removeCallback(this)
        super.onDestroy()
    }
}
