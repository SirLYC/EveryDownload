package com.lyc.everydownload

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo.IME_NULL
import androidx.core.widget.addTextChangedListener
import com.lyc.everydownload.util.requestForResult
import com.lyc.everydownload.util.toNormalUrl
import kotlinx.android.synthetic.main.layout_submit.view.*
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
            R.id.bt_choose_path, R.id.et_path -> {
                if (choosePathAction == null) {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "*/*"
                    Intent.createChooser(intent, "选择文件夹")
                    v.context.requestForResult(intent, 1) { _, data ->
                        println("${data?.data}")
                    }
                } else {
                    choosePathAction.invoke()
                }
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

        var filename = filenameEdit.text?.trim()?.toString()

        if (filename?.isEmpty() == true) {
            filename = null
        }
        startDownloadAction(url.toString(), pathEdit.text.toString().trim(), filename)
    }

    @SuppressLint("InflateParams")
    val view = LayoutInflater.from(context).inflate(R.layout.layout_submit, null).also {
        it.bt_start_download.setOnClickListener(this)
        it.bt_choose_path.setOnClickListener(this)
        it.et_path.setOnClickListener(this)
    }
    private val urlLayout = view.til_url
    private val urlEdit = view.et_url.apply {
        addTextChangedListener {
            urlLayout.isErrorEnabled = false
        }
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == IME_NULL || actionId == resources.getInteger(R.integer.start_download_id)) {
                handleSubmit()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }
    }
    private val filenameEdit = view.et_filename
    private val pathEdit = view.et_path
}
