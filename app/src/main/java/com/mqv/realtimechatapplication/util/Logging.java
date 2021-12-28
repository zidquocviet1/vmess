package com.mqv.realtimechatapplication.util;

import android.util.Log;

public class Logging {
    private static final String TAG = "TAC";

    public static void show(String msg){
        Log.d(TAG, msg);
    }

    public static void debug(String tag, String msg) {
        Log.d(tag, msg);
    }

    public static void info(String tag, String msg) {
        Log.i(tag, msg);
    }
}
