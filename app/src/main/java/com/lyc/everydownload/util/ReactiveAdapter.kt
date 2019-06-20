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
import kotlin.collections.set

/**
 * Created by Liu Yuchuan on 2019/5/20.
 */
class ReactiveAdapter(list: ObservableList<Any>) : MultiTypeAdapter(list), ListUpdateCallback {
    @Volatile
    private var gen = 0
    private val genToCallback = SparseArray<DiffUtil.ItemCallback<Any>>()
    private val callbackToGen = HashMap<DiffUtil.ItemCallback<Any>, Int>()
    /**
     * Decided by performance of [DiffUtil] and your data's complication
     * https://developer.android.com/reference/android/support/v7/util/DiffUtil
     */
    var useBackgroudThreadThreshold = 100
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

    /**
     * Note: this method may take time to refresh list
     * If you need show it more quickly without animation,
     * just replace [list] and use [RecyclerView.Adapter.notifyDataSetChanged]
     */
    fun replaceList(newList: ObservableList<Any>, cb: DiffUtil.ItemCallback<Any>, detectMoves: Boolean = false) {
        if (newList === this.list) {
            return
        }
        gen++

        if (list.size < useBackgroudThreadThreshold) {
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return cb.areItemsTheSame(list[oldItemPosition], newList[newItemPosition])
                }

                override fun getOldListSize(): Int {
                    return list.size
                }

                override fun getNewListSize(): Int {
                    return newList.size
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return cb.areContentsTheSame(list[oldItemPosition], newList[newItemPosition])
                }

            }, detectMoves)
            list = newList
            diffResult.dispatchUpdatesTo(this as RecyclerView.Adapter<*>)
            dispatchCommitActions(newList, list)
        } else {
            val exeGen = gen
            genToCallback.put(gen, cb)
            callbackToGen[cb] = gen
            // to make thread safe
            val oldList = list
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
                    if (exeGen == gen && genToCallback[gen] != null) {
                        list = newList
                        diffResult.dispatchUpdatesTo(this as RecyclerView.Adapter<*>)
                        dispatchCommitActions(newList, oldList)
                    }
                    removeItemCallback(cb)
                }
            }

        }
    }

    private fun dispatchCommitActions(newList: ObservableList<Any>, oldList: ObservableList<Any>) {
        if (replaceCommitActions.isNotEmpty()) {
            val newListCopy = newList.toList()
            for (replaceCommitAction in replaceCommitActions) {
                replaceCommitAction.onCommitReplace(oldList, newListCopy)
            }
        }
    }

    fun removeItemCallback(cb: DiffUtil.ItemCallback<Any>) {
        var index: Int
        do {
            index = genToCallback.indexOfValue(cb)
            if (index != -1) {
                genToCallback.removeAt(index)
            }
        } while (index != -1)
    }

    fun addReplaceCommitAction(replaceCommitAction: ReplaceCommitAction) {
        this.replaceCommitActions.add(replaceCommitAction)
    }

    fun removeReplaceCommitAction(replaceCommitAction: ReplaceCommitAction) {
        this.replaceCommitActions.remove(replaceCommitAction)
    }

    interface ReplaceCommitAction {
        /**
         * [newList] is a copy of new list
         */
        fun onCommitReplace(oldList: ObservableList<Any>, newList: List<Any>)
    }
}
