package com.mqv.realtimechatapplication.network.model.type;

import com.google.firebase.firestore.PropertyName;
import com.google.gson.annotations.SerializedName;

public enum MessageType {
    @SerializedName("generic")
    @PropertyName("generic")
    GENERIC,
    @SerializedName("share")
    @PropertyName("share")
    SHARE,
    @SerializedName("call")
    @PropertyName("call")
    CALL
}
