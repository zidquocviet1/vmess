package com.mqv.vmess.network.adapter

import com.google.gson.*
import org.apache.commons.codec.binary.Base64
import java.lang.reflect.Type

class ByteArrayAdapter : JsonDeserializer<ByteArray>, JsonSerializer<ByteArray> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext?
    ): ByteArray =
        Base64.decodeBase64(json.asString)

    override fun serialize(
        src: ByteArray?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement =
        JsonPrimitive(Base64.encodeBase64String(src))
}