package com.lyc.everydownload.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * @author liuyuchuan
 * @date 2019-06-19
 * @email kevinliu.sir@qq.com
 */

typealias requestResultAction = (resultCode: Int, data: Intent?) -> Unit

const val TAG = "REQUEST_FOR_RESULT_FRAGMENT"

fun Fragment.requestForResult(intent: Intent, requestCode: Int, action: requestResultAction) {
    val activity = activity
    if (activity == null) {
        logW("Fragment.requestForResult(), activity == null")
    } else {
        activity.requestForResult(intent, requestCode, action)
    }
}

fun FragmentActivity.requestForResult(intent: Intent, requestCode: Int, action: requestResultAction) {
    val fm = supportFragmentManager
    val resultProxyFragment = fm.findFragmentByTag(TAG) as? RequestForResultProxyFragment
            ?: RequestForResultProxyFragment().apply {
                fm.beginTransaction()
                        .add(this, TAG)
                        .commitNow()
            }
    resultProxyFragment.requestActionsMap.put(requestCode, action)
    resultProxyFragment.startActivityForResult(intent, requestCode)
}

fun Context.requestForResult(intent: Intent, requestCode: Int, action: requestResultAction) {
    getFragmentActivity()?.requestForResult(intent, requestCode, action)
}

class RequestForResultProxyFragment : Fragment() {
    val requestActionsMap = SparseArray<requestResultAction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val function = requestActionsMap[requestCode]
        requestActionsMap.remove(requestCode)
        function?.invoke(resultCode, data)
    }
}
