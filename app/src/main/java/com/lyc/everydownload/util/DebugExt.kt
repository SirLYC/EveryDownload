package com.lyc.everydownload.util

import android.util.Log
import com.lyc.everydownload.BuildConfig

/**
 * @author liuyuchuan
 * @date 2019-06-12
 * @email kevinliu.sir@qq.com
 */
inline fun runIfDebug(block: () -> Unit) {
    if (BuildConfig.DEBUG) {
        block()
    }
}

inline fun <reified T> T.logV(msg: Any, tr: Throwable? = null) {
    runIfDebug {
        Log.v(T::class.java.simpleName, "$msg", tr)
    }
}

inline fun <reified T> T.logD(msg: Any, tr: Throwable? = null) {
    runIfDebug {
        Log.d(T::class.java.simpleName, "$msg", tr)
    }
}

inline fun <reified T> T.logI(msg: Any, tr: Throwable? = null) {
    runIfDebug {
        Log.i(T::class.java.simpleName, "$msg", tr)
    }
}

inline fun <reified T> T.logW(msg: Any, tr: Throwable? = null) {
    runIfDebug {
        Log.w(T::class.java.simpleName, "$msg", tr)
    }
}

inline fun <reified T> T.logE(msg: Any, tr: Throwable? = null) {
    runIfDebug {
        Log.e(T::class.java.simpleName, "$msg", tr)
    }
}


inline fun <reified T> T.logWtf(msg: Any, tr: Throwable? = null) {
    runIfDebug {
        Log.wtf(T::class.java.simpleName, "$msg", tr)
    }
}
