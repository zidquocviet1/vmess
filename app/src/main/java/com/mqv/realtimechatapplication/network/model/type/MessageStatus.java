package com.mqv.realtimechatapplication.network.model.type;

import com.google.firebase.firestore.PropertyName;
import com.google.gson.annotations.SerializedName;

public enum MessageStatus {
    @SerializedName("seen")
    @PropertyName("seen")
    SEEN,
    @SerializedName("received")
    @PropertyName("received")
    RECEIVED,
    @SerializedName("not_received")
    @PropertyName("not_received")
    NOT_RECEIVED,
    @SerializedName("sending")
    @PropertyName("sending")
    SENDING,
    @SerializedName("error")
    @PropertyName("error")
    ERROR
}
