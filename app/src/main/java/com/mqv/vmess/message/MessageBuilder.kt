package com.mqv.vmess.message

import com.mqv.vmess.data.repository.KeyRepository
import com.mqv.vmess.network.exception.PreKeyNotFoundException
import com.mqv.vmess.network.model.PreKeyResponse
import com.mqv.vmess.reactive.RxHelper.parseResponseData
import com.mqv.vmess.util.Logging
import org.apache.commons.codec.binary.Base64
import org.signal.libsignal.protocol.*
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.SignalProtocolStore
import java.io.IOException
import java.util.*
import kotlin.NoSuchElementException

class MessageBuilder(
    private val store: SignalProtocolStore,
    private val keyRepository: KeyRepository
) {
    fun buildEncryptedMessage(participant: String, deviceId: Int, content: String): String {
        val remoteAddress = SignalProtocolAddress(participant, deviceId)
        val cipher = SessionCipher(store, remoteAddress)

        if (!store.containsSession(remoteAddress)) {
            try {
                for (bundle in getPreKeys(participant, deviceId)) {
                    val preKeyAddress = SignalProtocolAddress(participant, bundle.deviceId)
                    val sessionBuilder = SessionBuilder(store, preKeyAddress)
                    sessionBuilder.process(bundle)
                }
            } catch (e: InvalidKeyException) {
                throw IOException(e)
            } catch (e: UntrustedIdentityException) {
                throw UntrustedIdentityException("Untrusted identity key: $participant")
            } catch (e: NoSuchElementException) {
                throw PreKeyNotFoundException()
            }
        }

        val message = cipher.encrypt(content.toByteArray())

        return Base64.encodeBase64String(message.serialize())
    }

    private fun getPreKeys(participant: String, deviceId: Int): List<PreKeyBundle> {
        Logging.debug(
            TAG,
            "Fetching preKeyBundles for $participant, deviceId $deviceId"
        )

        val response: PreKeyResponse = keyRepository.getPreKey(participant, deviceId)
            .compose(parseResponseData())
            .onErrorComplete()
            .blockingSingle()

        val bundles: MutableList<PreKeyBundle> = LinkedList()

        for (item in response.preKeyItems) {
            var preKey: ECPublicKey? = null
            var signedPreKey: ECPublicKey? = null
            var signature: ByteArray? = null
            var preKeyId = -1
            var signedPreKeyId = -1

            if (item.preKey != null) {
                preKeyId = item.preKey.id
                preKey = item.preKey.publicKey
            }

            if (item.signedPreKey != null) {
                signedPreKeyId = item.signedPreKey.id
                signedPreKey = item.signedPreKey.publicKey
                signature = item.signedPreKey.signature
            }

            bundles.add(
                PreKeyBundle(
                    item.registrationId,
                    deviceId,
                    preKeyId,
                    preKey,
                    signedPreKeyId,
                    signedPreKey,
                    signature,
                    response.identityKey
                )
            )
        }
        return bundles
    }

    companion object {
        private val TAG: String = MessageBuilder::class.java.simpleName
    }
}