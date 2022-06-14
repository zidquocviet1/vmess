package com.mqv.vmess.network.adapter

import com.google.gson.*
import org.apache.commons.codec.binary.Base64
import org.signal.libsignal.protocol.IdentityKey
import java.lang.reflect.Type

class IdentityKeyAdapter : JsonSerializer<IdentityKey>, JsonDeserializer<IdentityKey> {
    override fun serialize(
        src: IdentityKey,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement =
        JsonPrimitive(Base64.encodeBase64String(src.serialize()))

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): IdentityKey =
        IdentityKey(Base64.decodeBase64(json.asString), 0)
}