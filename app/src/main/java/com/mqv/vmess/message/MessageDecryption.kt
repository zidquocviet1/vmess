package com.mqv.vmess.message

import com.mqv.vmess.dependencies.AppDependencies
import org.apache.commons.codec.binary.Base64
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage

object MessageDecryption {
    @JvmStatic
    fun decrypt(encodedMessage: String, participant: String, deviceId: Int): String {
        val sessionStore = AppDependencies.getLocalStorageSessionStore()
        val remoteAddress = SignalProtocolAddress(participant, deviceId)
        val sessionCipher = SessionCipher(sessionStore, remoteAddress)

        val signalMessage = PreKeySignalMessage(Base64.decodeBase64(encodedMessage))
        val decryptedMessage = sessionCipher.decrypt(signalMessage)
        val readableMessage = String(decryptedMessage)

        return readableMessage
    }
}