package com.lyc.everydownload.util.rv

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

/**
 * Created by Liu Yuchuan on 2019/6/20.
 */
fun RecyclerView.closeAllAnimation() {
    itemAnimator?.let { itemAnimator ->
        itemAnimator.addDuration = 0
        itemAnimator.changeDuration = 0
        itemAnimator.removeDuration = 0
        itemAnimator.moveDuration = 0

    }

    (itemAnimator as? SimpleItemAnimator)?.let { simpleItemAnimator ->
        simpleItemAnimator.supportsChangeAnimations = false
    }
}
