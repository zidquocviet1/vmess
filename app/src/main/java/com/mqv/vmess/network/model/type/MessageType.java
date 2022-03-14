package com.mqv.vmess.network.model.type;

import com.google.gson.annotations.SerializedName;

public enum MessageType {
    @SerializedName("generic")
    GENERIC,
    @SerializedName("share")
    SHARE,
    @SerializedName("call")
    CALL
}
