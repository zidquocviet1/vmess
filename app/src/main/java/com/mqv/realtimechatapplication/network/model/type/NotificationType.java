package com.mqv.realtimechatapplication.network.model.type;

import com.google.gson.annotations.SerializedName;

public enum NotificationType {
    @SerializedName("new_friend_request")
    NEW_FRIEND_REQUEST,
    @SerializedName("accepted_friend_request")
    ACCEPTED_FRIEND_REQUEST
}
