package com.mqv.realtimechatapplication.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public final class NetworkConstraint {
    public static boolean isMet(Context context) {
        ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);
        NetworkInfo         networkInfo         = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }
}
