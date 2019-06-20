package com.lyc.everydownload.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.lyc.everydownload.Async
import me.drakeet.multitype.MultiTypeAdapter
import java.util.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

/**
 * Created by Liu Yuchuan on 2019/5/20.
 */
class ReactiveAdapter(list: ObservableList<Any>) : MultiTypeAdapter(list), ListUpdateCallback {
    @Volatile
    private var gen = 0
    private val genToCallback = SparseArray<DiffUtil.ItemCallback<Any>>()
    private val callbackToGen = HashMap<DiffUtil.ItemCallback<Any>, Int>()
    private val replaceCommitActions = mutableListOf<ReplaceCommitAction>()

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

    fun replaceList(newList: List<Any>, cb: DiffUtil.ItemCallback<Any>, detectMoves: Boolean = false) {
        if (newList == this.list) {
            return
        }

        gen++
        val exeGen = gen
        genToCallback.put(gen, cb)
        callbackToGen[cb] = gen
        // to make thread safe
        val oldList = ArrayList(list)
        Async.computation.execute {
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return if (gen != exeGen) {
                        true
                    } else {
                        genToCallback[gen]?.areItemsTheSame(oldList[oldItemPosition], newList[newItemPosition])
                                ?: true
                    }
                }

                override fun getOldListSize(): Int {
                    return oldList.size
                }

                override fun getNewListSize(): Int {
                    return newList.size
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return if (gen != exeGen) {
                        true
                    } else {
                        genToCallback[gen]?.areContentsTheSame(oldList[oldItemPosition], newList[newItemPosition])
                                ?: true
                    }
                }

            }, detectMoves)

            Async.main.execute {
                if (exeGen == gen) {
                    list.enable = false
                    list.clear()
                    list.addAll(newList)
                    list.enable = true
                    diffResult.dispatchUpdatesTo(this as RecyclerView.Adapter<*>)
                    for (replaceCommitAction in replaceCommitActions) {
                        replaceCommitAction.onCommitReplace()
                    }
                }
                remove(cb)
            }
        }
    }

    fun remove(cb: DiffUtil.ItemCallback<Any>) {
        callbackToGen.remove(cb)?.let {
            genToCallback.remove(it)
        }
    }

    fun addReplaceCommitAction(replaceCommitAction: ReplaceCommitAction) {
        this.replaceCommitActions.add(replaceCommitAction)
    }

    fun removeReplaceCommitAction(replaceCommitAction: ReplaceCommitAction) {
        this.replaceCommitActions.remove(replaceCommitAction)
    }

    interface ReplaceCommitAction {
        fun onCommitReplace()
    }
}
