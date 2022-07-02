package com.mqv.vmess.util

import android.Manifest
import android.content.Context
import android.os.Build
import com.mqv.vmess.ui.permissions.Permission

object StorageUtil {
    fun canReadDataFromMediaStore(context: Context) =
//        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                Permission.hasAll(context, Manifest.permission.READ_EXTERNAL_STORAGE)
}