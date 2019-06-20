package com.lyc.everydownload.file

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo.IME_NULL
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lyc.everydownload.R
import kotlinx.android.synthetic.main.dialog_input.view.*


/**
 * @author liuyuchuan
 * @date 2019-06-20
 * @email kevinliu.sir@qq.com
 */
class MkdirDialog : DialogFragment() {

    private lateinit var editText: TextInputEditText
    private lateinit var textInputLayout: TextInputLayout
    private lateinit var fileExploreViewModel: FileExploreViewModel

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = context!!
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_input, null)
        editText = view.edit
        textInputLayout = view.til
        textInputLayout.hint = ctx.getString(R.string.input_new_dir_name)
        editText.doAfterTextChanged {
            textInputLayout.isErrorEnabled = false
        }
        editText.setImeActionLabel(getString(R.string.create), 20)
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == IME_NULL || actionId == 20) {
                handleCreate()
                return@setOnEditorActionListener true
            }
            false
        }

        fileExploreViewModel = ViewModelProviders.of(activity!!).get(FileExploreViewModel::class.java)

        val dialog = AlertDialog.Builder(ctx)
                .setTitle(R.string.mk_dir)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.create, null)
                .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                handleCreate()
            }
        }
        return dialog
    }

    private fun handleCreate() {
        val result = fileExploreViewModel.mkDir(editText.text.toString().trim())
        if (result == null) {
            fileExploreViewModel.successMsg.postValue("创建成功")
            dismissAllowingStateLoss()
        } else {
            textInputLayout.error = result
        }
    }
}
