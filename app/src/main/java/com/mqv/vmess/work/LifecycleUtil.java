package com.mqv.vmess.work;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

import android.app.ActivityManager;

public final class LifecycleUtil {
    /*
    * Check if the app is running in foreground or not
    * */
    public static boolean isAppForeground() {
        /*
         * Detail link: {https://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not}
         * */
        var runningAppInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(runningAppInfo);

        return runningAppInfo.importance == IMPORTANCE_FOREGROUND ||
                runningAppInfo.importance == IMPORTANCE_VISIBLE;
    }
}
