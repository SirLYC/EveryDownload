package com.lyc.everydownload.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewStub
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.annotation.LayoutRes
import androidx.core.view.children
import com.lyc.everydownload.R
import kotlinx.android.synthetic.main.layout_multi_state_view.view.*

/**
 * @author liuyuchuan
 * @date 2019-06-21
 * @email kevinliu.sir@qq.com
 */
class MultiStateView : FrameLayout {
    private val stubs: Array<ViewStub>
    private val stubSet: Set<ViewStub>

    @StateInt
    var currentState: Int = STATE_NO_STATE
        private set

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [STATE_NO_STATE, STATE_EMPTY, STATE_LOADING, STATE_ERROR])
    annotation class StateInt


    companion object {
        const val STATE_NO_STATE = -1
        const val STATE_EMPTY = 0
        const val STATE_LOADING = 1
        const val STATE_ERROR = 2
    }

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet?, style: Int) : super(context, attributeSet, style) {
        LayoutInflater.from(context).inflate(R.layout.layout_multi_state_view, this)
        stubs = arrayOf(error_view, empty_view, loading_view)
        stubSet = hashSetOf(*stubs)

        if (attributeSet != null) {
            val ta = context.obtainStyledAttributes(attributeSet, R.styleable.MultiStateView)
            ta?.run {
                stubs[0].layoutResource =
                        getResourceId(R.styleable.MultiStateView_empty_view, R.layout.layout_empty_default)
                stubs[1].layoutResource =
                        getResourceId(R.styleable.MultiStateView_loading_view, R.layout.layout_loading_default)
                stubs[2].layoutResource =
                        getResourceId(R.styleable.MultiStateView_error_view, R.layout.layout_error_default)
                recycle()
            }
        }

        if (stubs[0].layoutResource == 0) {
            stubs[0].layoutResource = R.layout.layout_empty_default
        }

        if (stubs[1].layoutResource == 0) {
            stubs[1].layoutResource = R.layout.layout_loading_default
        }

        if (stubs[2].layoutResource == 0) {
            stubs[2].layoutResource = R.layout.layout_error_default
        }

    }

    fun showState(@StateInt state: Int) {
        val viewStub = getStubByState(state)
        children.forEach {
            it.visibility = View.GONE
        }
        viewStub.visibility = View.VISIBLE
        currentState = state
    }

    fun showContent() {
        children.forEach {
            if (!stubSet.contains(it)) {
                it.visibility = View.VISIBLE
            }
        }
        stubs.forEach {
            it.visibility = View.GONE
        }
        currentState = STATE_NO_STATE
    }

    fun setOnInflateListener(listener: ViewStub.OnInflateListener) {
        stubs.forEach {
            it.setOnInflateListener(listener)
        }
    }

    fun setInflateRes(@StateInt state: Int, @LayoutRes layoutRes: Int) {
        getStubByState(state).layoutResource = layoutRes
    }

    private fun getStubByState(state: Int): ViewStub {
        return stubs[state]
    }
}
