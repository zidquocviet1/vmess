package com.mqv.vmess.ui.permissions

import android.content.pm.PackageManager
import java.util.function.Consumer

// Entry point to detect the result from user after dialogs are requested
// And check whether the event is needed to run
class PermissionRequest(
    private var allGrantedListener: Runnable? = null,
    private var anyDeniedListener: Runnable? = null,
    private var anyPermanentlyDeniedListener: Runnable? = null,
    private var anyResultListener: Runnable? = null,
    private var someGrantedListener: Consumer<List<String>>? = null,
    private var someDeniedListener: Consumer<List<String>>? = null,
    private var somePermanentlyDeniedListener: Consumer<List<String>>? = null
) {
    private val preRequestMapping = mutableMapOf<String, Boolean>()

    fun onResult(
        permissions: Array<String>,
        grantResults: IntArray,
        shouldShowRationaleDialog: BooleanArray
    ) {
        val granted = mutableListOf<String>()
        val denied = mutableListOf<String>()
        val permanentlyDenied = mutableListOf<String>()

        for (index in permissions.indices) {
            if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                granted.add(permissions[index])
            } else {
                preRequestMapping[permissions[index]]?.let { preRequestShouldShowRationaleDialog ->
                    if ((somePermanentlyDeniedListener != null || anyPermanentlyDeniedListener != null) &&
                        !preRequestShouldShowRationaleDialog && !shouldShowRationaleDialog[index]
                    ) {
                        permanentlyDenied.add(permissions[index])
                    } else {
                        denied.add(permissions[index])
                    }
                }

            }
        }

        if (allGrantedListener != null && granted.size > 0 && (denied.size == 0 && permanentlyDenied.size == 0)) {
            allGrantedListener?.run()
        } else if (someGrantedListener != null && granted.size > 0) {
            someGrantedListener?.accept(granted)
        }

        if (denied.size > 0) {
            anyDeniedListener?.run()
            someDeniedListener?.accept(denied)
        }

        if (permanentlyDenied.size > 0) {
            anyPermanentlyDeniedListener?.run()
            somePermanentlyDeniedListener?.accept(permanentlyDenied)
        }

        anyResultListener?.run()
    }

    fun addMapping(permission: String, shouldShowRationaleDialog: Boolean) {
        preRequestMapping[permission] = shouldShowRationaleDialog
    }
}