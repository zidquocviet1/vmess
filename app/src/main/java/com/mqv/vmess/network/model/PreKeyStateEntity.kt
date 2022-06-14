package com.mqv.vmess.network.model

import com.google.gson.annotations.JsonAdapter
import com.mqv.vmess.network.adapter.IdentityKeyAdapter
import org.signal.libsignal.protocol.IdentityKey

data class PreKeyStateEntity(
    @JsonAdapter(value = IdentityKeyAdapter::class)
    val identityKey: IdentityKey,
    val preKeys: List<PreKeyEntity>,
    val signedPreKey: SignedPreKeyEntity
)