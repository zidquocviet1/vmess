package com.mqv.vmess.network.model

import org.signal.libsignal.protocol.ecc.ECPublicKey

open class PreKeyEntity(
    val id: Int,
    val publicKey: ECPublicKey
)