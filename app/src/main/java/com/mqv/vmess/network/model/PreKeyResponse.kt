package com.mqv.vmess.network.model

import com.google.gson.annotations.JsonAdapter
import com.mqv.vmess.network.adapter.IdentityKeyAdapter
import org.signal.libsignal.protocol.IdentityKey

data class PreKeyResponse(
    @JsonAdapter(value = IdentityKeyAdapter::class)
    val identityKey: IdentityKey,
    val preKeyItems: List<PreKeyResponseItem>,
)