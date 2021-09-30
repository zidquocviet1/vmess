package com.mqv.realtimechatapplication.network.model.type;

import com.google.gson.annotations.SerializedName;

public enum FriendRequestStatus {
    @SerializedName("confirm")
    CONFIRM,
    @SerializedName("pending")
    PENDING,
    @SerializedName("request")
    REQUEST,
    @SerializedName("acknowledge")
    ACKNOWLEDGE,
    @SerializedName("cancel")
    CANCEL
}
