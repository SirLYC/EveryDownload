package com.lyc.everydownload.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

/**
 * Created by Liu Yuchuan on 2018/8/16.
 */
public final class BlockDetectByPrinter {

    private static final String START = ">>>>> Dispatching";
    private static final String END = "<<<<< Finished";

    private static final String TAG = "BlockDetectByPrinter";

    // 300ms
    private static final long TIME_BLOCK = 300;
    private static final Runnable logRunnable = () -> {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = Looper.getMainLooper().getThread().getStackTrace();
        for (StackTraceElement stackTraceElement : stackTrace) {
            sb.append(stackTraceElement.toString()).append('\n');
        }

        Log.w(TAG, sb.toString());
    };

    private static final Handler handler;

    static {
        HandlerThread handlerThread = new HandlerThread("log");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    private BlockDetectByPrinter() { }

    public static void start() {
        Looper.getMainLooper().setMessageLogging(x -> {
            if (x.startsWith(START)) {
                handler.postDelayed(logRunnable, TIME_BLOCK);
            }

            if (x.startsWith(END)) {
                handler.removeCallbacks(logRunnable);
            }
        });
    }
}
