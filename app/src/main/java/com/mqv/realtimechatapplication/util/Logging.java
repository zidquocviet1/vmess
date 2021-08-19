package com.mqv.realtimechatapplication.util;

import android.util.Log;

public class Logging {
    private static final String TAG = "TAC";

    public static void show(String msg){
        Log.d(TAG, msg);
    }
}
