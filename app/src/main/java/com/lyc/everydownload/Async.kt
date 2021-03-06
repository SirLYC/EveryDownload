package com.lyc.everydownload

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Created by Liu Yuchuan on 2019/5/18.
 */
object Async {
    internal val cache = Executors.newCachedThreadPool()
    private val HANDLER = Handler(Looper.getMainLooper())
    internal val main: Executor = Executor { command -> HANDLER.post(command) }
    internal val instantMain = Executor { command ->
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            command.run()
        } else {
            HANDLER.post(command)
        }
    }
    internal val computation = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1)
}
