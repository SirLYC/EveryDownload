package com.lyc.everydownload

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.lyc.everydownload.util.doWithRWPermission

/**
 * Created by Liu Yuchuan on 2019/6/17.
 * Only in MainActivity
 */
class StartDownloadDialog : DialogFragment() {

    private lateinit var mainViewModel: MainViewModel
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = context!!
        mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
        return AlertDialog.Builder(ctx)
                .setView(StartDownloadViewPack(ctx, this::startDownloadAction).view)
                .create()
    }

    private fun startDownloadAction(url: String, path: String, filename: String?) {
        activity?.doWithRWPermission(
                {
                    // TODO: 2019/6/17 path choose
                    mainViewModel.submit(url, context!!.externalCacheDir!!.absolutePath, filename)
                    dismiss()
                },
                {
                    startDownloadAction(url, path, filename)
                }
        )
    }
}
