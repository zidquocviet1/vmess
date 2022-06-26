package com.mqv.vmess.message

import androidx.annotation.WorkerThread
import com.mqv.vmess.data.repository.KeyRepository
import com.mqv.vmess.network.exception.PreKeyNotFoundException
import com.mqv.vmess.network.model.PreKeyResponse
import com.mqv.vmess.reactive.RxHelper.parseResponseData
import com.mqv.vmess.util.Logging
import io.reactivex.rxjava3.core.Single
import org.apache.commons.codec.binary.Base64
import org.signal.libsignal.protocol.*
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.SignalProtocolStore
import java.io.IOException
import java.util.*

class MessageBuilder(
    private val store: SignalProtocolStore,
    private val keyRepository: KeyRepository
) {
    @WorkerThread
    fun buildEncryptedMessage(participant: String, deviceId: Int, content: String): Single<String> {
        val remoteAddress = SignalProtocolAddress(participant, deviceId)
        val cipher = SessionCipher(store, remoteAddress)

        return getPreKeys(participant, deviceId).flatMap { bundles ->
            if (!store.containsSession(remoteAddress)) {
                try {
                    for (bundle in bundles) {
                        val preKeyAddress = SignalProtocolAddress(participant, bundle.deviceId)
                        val sessionBuilder = SessionBuilder(store, preKeyAddress)
                        sessionBuilder.process(bundle)
                    }
                } catch (e: InvalidKeyException) {
                    Single.error<Exception>(IOException(e))
                } catch (e: UntrustedIdentityException) {
                    Single.error<Exception>(UntrustedIdentityException("Untrusted identity key: $participant"))
                } catch (e: NoSuchElementException) {
                    Single.error<Exception>(PreKeyNotFoundException())
                }
            }
            Single.just(Base64.encodeBase64String(cipher.encrypt(content.toByteArray()).serialize()))
        }
    }

    @WorkerThread
    private fun getPreKeys(participant: String, deviceId: Int): Single<List<PreKeyBundle>> {
        Logging.debug(
            TAG,
            "Fetching preKeyBundles for $participant, deviceId $deviceId on thread: ${Thread.currentThread().name}"
        )

        return keyRepository.getPreKey(participant, deviceId)
            .compose(parseResponseData())
            .doOnError { t -> Logging.debug(TAG, "Can't get preKeyBundle because: ${t.message}") }
            .singleOrError()
            .map { response -> getBundleFromPreKeyResponse(response, deviceId) }
    }

    private fun getBundleFromPreKeyResponse(response: PreKeyResponse, deviceId: Int): List<PreKeyBundle> {
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