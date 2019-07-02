package com.lyc.everydownload

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.lyc.downloader.DownloadTask
import com.lyc.downloader.utils.DownloadStringUtil
import com.lyc.everydownload.util.toDateString
import kotlinx.android.synthetic.main.layout_item_content.view.*

/**
 * @author liuyuchuan
 * @date 2019-07-02
 * @email kevinliu.sir@qq.com
 */
class ItemContentDialog : DialogFragment() {
    var item: DownloadItem? = null

    companion object {
        fun show(fm: FragmentManager, downloadItem: DownloadItem) {
            val dialog = ItemContentDialog()
            val bundle = Bundle()
            bundle.putParcelable("KEY_ITEM", downloadItem)
            dialog.arguments = bundle
            dialog.show(fm, ItemContentDialog::class.java.simpleName)
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = context!!
        val item = if (savedInstanceState != null) {
            savedInstanceState.getParcelable<DownloadItem>("KEY_ITEM")
        } else {
            arguments?.getParcelable<DownloadItem>("KEY_ITEM")
        }

        val view = LayoutInflater.from(ctx).inflate(R.layout.layout_item_content, null)
        if (item == null) {
            view.tv_content.text = getString(R.string.error_show_content)
        } else {
            view.tv_content.text = item.run {
                "${getString(R.string.filename)}: ${item.filename}\n" +
                        "${getString(R.string.path)}: ${item.path}\n" +
                        "${getString(R.string.title_url)}: ${item.url}\n" +
                        "${getString(R.string.filne_length)}: ${if (item.totalSize <= 0) "未知" else DownloadStringUtil.byteToString(item.totalSize.toDouble())}\n" +
                        "${getString(R.string.create_time)}: ${createdTime.toDateString()}\n" +
                        if (downloadState == DownloadTask.FINISH && finishedTime != null) {
                            "完成时间: finishedTime!!.toDateString()"
                        } else {
                            ""
                        }
            }
        }

        this.item = item

        return AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.download_content))
                .setView(view)
                .setPositiveButton(R.string.finish, null)
                .create()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (item != null) {
            outState.putParcelable("KEY_ITEM", item)
        }
    }
}
