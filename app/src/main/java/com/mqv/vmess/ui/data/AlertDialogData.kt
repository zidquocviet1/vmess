package com.mqv.vmess.ui.data

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlertDialogData(
    @StringRes val title: Int,
    @StringRes val message: Int,
    @StringRes val positiveButton: Int,
    @StringRes val negativeButton: Int,
    val type: Type
) : Parcelable

enum class Type {
    DELETE,
    LEAVE_GROUP
}