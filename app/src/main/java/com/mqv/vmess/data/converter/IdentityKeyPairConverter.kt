package com.mqv.vmess.data.converter

import androidx.room.TypeConverter
import org.apache.commons.codec.binary.Base64
import org.signal.libsignal.protocol.IdentityKeyPair

class IdentityKeyPairConverter {
    // Decode identity key pair to Base64
    @TypeConverter
    fun fromIdentityKeyPair(pair: IdentityKeyPair): String =
        Base64.encodeBase64String(pair.serialize())

    // Encode encoded Base64 to identity key pair
    @TypeConverter
    fun toIdentityKeyPair(encodedKey: String): IdentityKeyPair =
        IdentityKeyPair(Base64.decodeBase64(encodedKey))
}