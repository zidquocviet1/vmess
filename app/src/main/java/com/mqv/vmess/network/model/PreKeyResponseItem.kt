package com.mqv.vmess.network.model

data class PreKeyResponseItem(
    val registrationId: Int,
    val deviceId: Int,
    val preKey: PreKeyEntity?,
    val signedPreKey: SignedPreKeyEntity?,
)