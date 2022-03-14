package com.mqv.vmess.network.model.type;

import com.google.gson.annotations.SerializedName;

public enum MessageStatus {
    @SerializedName("seen")
    SEEN,
    @SerializedName("received")
    RECEIVED,
    @SerializedName("not_received")
    NOT_RECEIVED,
    @SerializedName("sending")
    SENDING,
    @SerializedName("error")
    ERROR
}
