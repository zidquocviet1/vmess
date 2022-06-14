package com.mqv.vmess.network.model

import com.google.gson.annotations.JsonAdapter
import com.mqv.vmess.network.adapter.ByteArrayAdapter
import org.signal.libsignal.protocol.ecc.ECPublicKey

class SignedPreKeyEntity(
    id: Int,
    publicKey: ECPublicKey,
    @JsonAdapter(value = ByteArrayAdapter::class) val signature: ByteArray
) :
    PreKeyEntity(id, publicKey) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignedPreKeyEntity

        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        return signature.contentHashCode()
    }
}