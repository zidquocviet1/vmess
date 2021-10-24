package com.mqv.realtimechatapplication.network.model.type;

import com.google.gson.annotations.SerializedName;

public enum ConversationStatusType {
    @SerializedName("inbox")
    INBOX,
    @SerializedName("archived")
    ARCHIVED,
    @SerializedName("request")
    REQUEST
}
