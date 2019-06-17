package com.lyc.everydownload.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ListUpdateCallback
import me.drakeet.multitype.MultiTypeAdapter

/**
 * Created by Liu Yuchuan on 2019/5/20.
 */
class ReactiveAdapter(list: ObservableList<Any>) : MultiTypeAdapter(list), ListUpdateCallback {

    var list: ObservableList<Any> = list
        set(value) {
            if (field !== value) {
                field = value
                items = value
                field.removeCallback(this)
                value.addCallback(this)
            }
        }

    override fun onInserted(position: Int, count: Int) = notifyItemRangeInserted(position, count)
    override fun onRemoved(position: Int, count: Int) = notifyItemRangeRemoved(position, count)
    override fun onMoved(fromPosition: Int, toPosition: Int) = notifyItemMoved(fromPosition, toPosition)
    override fun onChanged(position: Int, count: Int, payload: Any?) = notifyItemRangeChanged(position, count, payload)

    fun observe(activity: Activity) {
        activity.application.registerActivityLifecycleCallbacks(ReactiveActivityRegistry(activity))
        list.addCallback(this)
    }

    fun observe(fragment: Fragment) {
        val fm = fragment.fragmentManager
        if (fm != null) {
            fm.registerFragmentLifecycleCallbacks(ReactiveFragmentRegistry(fragment), false)
            list.addCallback(this)
        }
    }

    inner class ReactiveActivityRegistry(
            private val activity: Activity
    ) : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivityDestroyed(activity: Activity) {
            if (this.activity == activity) {
                list.removeCallback(this@ReactiveAdapter)
                activity.application.unregisterActivityLifecycleCallbacks(this)
            }
        }
    }

    inner class ReactiveFragmentRegistry(
            private val fragment: Fragment
    ) : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
            super.onFragmentViewDestroyed(fm, f)
            if (fragment == f) {
                list.removeCallback(this@ReactiveAdapter)
                fm.unregisterFragmentLifecycleCallbacks(this)
            }
        }
    }
}
