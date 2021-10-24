package com.mqv.realtimechatapplication.network.model.type;

import com.google.gson.annotations.SerializedName;

public enum ConversationType {
    @SerializedName("self")
    SELF,
    @SerializedName("normal")
    NORMAL,
    @SerializedName("group")
    GROUP
}