package com.mqv.vmess.network.adapter

import com.google.gson.*
import org.apache.commons.codec.binary.Base64
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECPublicKey
import java.io.IOException
import java.lang.reflect.Type

class ECPublicKeyAdapter : JsonSerializer<ECPublicKey>, JsonDeserializer<ECPublicKey> {
    override fun serialize(
        src: ECPublicKey,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement =
        JsonPrimitive(Base64.encodeBase64String(src.serialize()))

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ECPublicKey {
        return try {
            Curve.decodePoint(Base64.decodeBase64(json.asString), 0)
        } catch (e: InvalidKeyException) {
            throw IOException(e)
        }
    }
}