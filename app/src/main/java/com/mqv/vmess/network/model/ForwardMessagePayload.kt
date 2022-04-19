package com.mqv.vmess.network.model

data class ForwardMessagePayload(
    var url: String,
    var thumbnail: String?,
    var type: Type
)