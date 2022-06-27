package com.mqv.vmess.message

import android.content.Context
import com.mqv.vmess.R
import com.mqv.vmess.dependencies.AppDependencies
import org.apache.commons.codec.binary.Base64
import org.signal.libsignal.protocol.*
import org.signal.libsignal.protocol.message.PreKeySignalMessage

object MessageDecryption {
    @JvmStatic
    fun decrypt(
        context: Context,
        encodedMessage: String,
        participant: String,
        deviceId: Int
    ): String {
        return try {
            val sessionStore = AppDependencies.getLocalStorageSessionStore()
            val remoteAddress = SignalProtocolAddress(participant, deviceId)
            val sessionCipher = SessionCipher(sessionStore, remoteAddress)

            val signalMessage = PreKeySignalMessage(Base64.decodeBase64(encodedMessage))
            val decryptedMessage = sessionCipher.decrypt(signalMessage)
            val readableMessage = String(decryptedMessage)

            readableMessage
        } catch (e: Exception) {
            context.getString(
                R.string.dummy_encrypted_message
            )

//            return when (e) {
//                is InvalidKeyIdException,
//                is UntrustedIdentityException,
//                is LegacyMessageException,
//                is DuplicateMessageException,
//                is InvalidMessageException ->
//                else -> encodedMessage
//            }
        }
    }
}