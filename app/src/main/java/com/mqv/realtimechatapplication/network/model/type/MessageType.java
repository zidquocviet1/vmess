package com.mqv.realtimechatapplication.network.model.type;

import com.google.gson.annotations.SerializedName;

public enum MessageType {
    @SerializedName("generic")
    GENERIC,
    @SerializedName("share")
    SHARE,
    @SerializedName("call")
    CALL
}
