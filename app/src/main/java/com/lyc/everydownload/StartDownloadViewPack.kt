package com.lyc.everydownload

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo.IME_NULL
import androidx.core.net.toFile
import androidx.core.widget.doAfterTextChanged
import com.lyc.everydownload.file.FileExploreActivity
import com.lyc.everydownload.preference.AppPreference
import com.lyc.everydownload.util.doWithRWPermission
import com.lyc.everydownload.util.requestForResult
import com.lyc.everydownload.util.toNormalUrl
import kotlinx.android.synthetic.main.layout_submit.view.*
import java.io.File
import java.util.regex.Pattern


/**
 * Created by Liu Yuchuan on 2019/6/17.
 */
class StartDownloadViewPack(context: Context,
                            private val startDownloadAction: (url: String, path: String, filename: String?) -> Unit,
                            private val choosePathAction: (() -> Unit)? = null
) : View.OnClickListener {

    private val pattern = Pattern.compile("((([hH][Tt][Tt][Pp][Ss]?)?:/?/?)?([a-zA-Z0-9]+[.])|([Ww][Ww][Ww].))\\w+[.|/]([a-zA-Z0-9]*)?[[.]([a-zA-Z0-9]{0,})]+((/[\\S&&[^,;\u4E00-\u9FA5]]+)+)?([.][a-zA-Z0-9]*+|/?)")
    internal var path: String = ""
        set(value) {
            field = value
            pathEdit.setText(value)
        }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.bt_start_download -> {
                handleSubmit()
            }
        }
    }

    private fun goToChoosePath(v: View) {
        if (choosePathAction == null) {
            v.context.doWithRWPermission({
                v.context.requestForResult(
                        Intent(v.context, FileExploreActivity::class.java).apply {
                            putExtra(FileExploreActivity.KEY_PATH, pathEdit.text.toString().trim())
                            putExtra(FileExploreActivity.KEY_DIR, true)
                        },
                        2,
                        action = this::handleResult
                )
            }, {
                goToChoosePath(v)
            })
        } else {
            choosePathAction.invoke()
        }

    }

    private fun handleResult(code: Int, data: Intent?) {
        if (code == Activity.RESULT_OK) {
            data?.data?.let {
                pathEdit.setText(it.toFile().canonicalPath)
            }
        }
    }

    private fun handleSubmit() {
        var url: CharSequence = urlEdit.text!!.toString()
        if (pattern.matcher(url).matches()) {
            url = url.toNormalUrl()
            urlEdit.setText(url)
        } else {
            urlLayout.error = urlLayout.context.getString(R.string.illegal_url)
            urlLayout.requestFocus()
            return
        }

        val path = pathEdit.text.toString().trim()
        val file = File(path)
        if (!file.exists() && file.mkdirs()) {
            pathLayout.error = urlLayout.context.getString(R.string.dir_not_exist)
            return
        } else if (!file.isDirectory) {
            pathLayout.error = urlLayout.context.getString(R.string.path_not_dir)
            return
        }

        var filename = filenameEdit.text?.trim()?.toString()

        if (filename?.isEmpty() == true) {
            filename = null
        }

        AppPreference.lastDownloadDir = path

        startDownloadAction(url.toString(), path, filename)
    }

    @SuppressLint("InflateParams")
    val view: View = LayoutInflater.from(context).inflate(R.layout.layout_submit, null).also {
        it.bt_start_download.setOnClickListener(this)
        it.til_path.setEndIconActivated(true)
    }
    private val urlLayout = view.til_url
    private val urlEdit = view.et_url.apply {
        doAfterTextChanged {
            urlLayout.isErrorEnabled = false
        }
    }
    private val filenameEdit = view.et_filename
    private val pathLayout = view.til_path.apply {
        setEndIconOnClickListener {
            goToChoosePath(this)
        }
    }
    private val pathEdit = view.et_path.apply {
        doAfterTextChanged {
            pathLayout.isErrorEnabled = false
        }

        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == IME_NULL || actionId == resources.getInteger(R.integer.start_download_id)) {
                handleSubmit()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }
    }

    init {
        val path = if (AppPreference.useLastDownloadDir) {
            AppPreference.lastDownloadDir ?: AppPreference.defaultDownloadDir
        } else {
            AppPreference.defaultDownloadDir
        }
        pathEdit.setText(path)
    }
}
