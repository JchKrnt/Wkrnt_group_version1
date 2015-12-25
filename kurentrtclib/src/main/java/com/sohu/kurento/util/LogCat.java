package com.sohu.kurento.util;

import android.util.Log;

/**
 * Created by jchsohu on 15-7-6.
 */
public class LogCat {

    private static boolean releaseAble = false;

    private static final String TAG = "SOHU_RTC";

    public static void setRleaseable(boolean release) {
        releaseAble = release;
    }

    public static void debug(String msg) {
        if (releaseAble) {
            return;
        } else
            Log.d(TAG, msg);
    }

    public static void e(String msg) {
        if (releaseAble) {
            return;
        } else
            Log.e(TAG, msg);
    }

    public static void e(String tag, String msg) {

        Log.e(tag, msg);
    }

    public static void v(String msg) {
        if (releaseAble) {
            return;
        } else
            Log.v(TAG, msg);
    }

    public static void i(String msg) {
        if (releaseAble) {
            return;
        } else
            Log.i(TAG, msg);
    }

    public static void w(String msg) {
        if (releaseAble) {
            return;
        } else
            Log.w(TAG, msg);
    }
}
