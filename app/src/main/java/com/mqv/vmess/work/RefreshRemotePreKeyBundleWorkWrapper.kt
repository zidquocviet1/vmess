package com.mqv.vmess.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import androidx.work.rxjava3.RxWorker
import com.mqv.vmess.data.repository.impl.KeyRepositoryImpl
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.model.PreKeyResponse
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.util.Logging
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Single
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.UntrustedIdentityException
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.state.PreKeyBundle
import java.util.*
import java.util.concurrent.TimeUnit

private const val KEY_REMOTE_ADDRESS = "remote_address"

class RefreshRemotePreKeyBundleWorkWrapper(context: Context, private val remoteId: String) :
    BaseWorker(context) {
    override fun retrieveConstraint(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    override fun isUniqueWork(): Boolean = false

    override fun createRequest(): WorkRequest =
        OneTimeWorkRequestBuilder<RefreshRemotePreKeyBundleWorker>()
            .setInputData(Data.Builder().putString(KEY_REMOTE_ADDRESS, remoteId).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 2, TimeUnit.SECONDS)
            .build()

    @HiltWorker
    class RefreshRemotePreKeyBundleWorker @AssistedInject constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val repository: KeyRepositoryImpl
    ) : RxWorker(appContext, workerParams) {
        override fun createWork(): Single<Result> {
            val remoteId = inputData.getString(KEY_REMOTE_ADDRESS)

            if (remoteId.isNullOrEmpty()) {
                throw IllegalArgumentException("User is not valid!!!")
            }

            // Remove all PreKeyBundle of that remote user session and then get fresh keys again
            AppDependencies.getLocalStorageSessionStore().deleteAllSessions(remoteId)

            return refreshPreKeyBundleRemoteUser(remoteId, deviceId = 1)
        }

        private fun refreshPreKeyBundleRemoteUser(
            participant: String,
            deviceId: Int
        ): Single<Result> {
            Logging.debug(
                TAG,
                "Fetching preKeyBundles for $participant, deviceId $deviceId on thread: ${Thread.currentThread().name}"
            )

            if (runAttemptCount < 3) {
                return repository.getPreKey(participant, deviceId)
                    .compose(RxHelper.parseResponseData())
                    .doOnError { t ->
                        Logging.debug(
                            TAG,
                            "Can't get preKeyBundle because: ${t.message}"
                        )
                    }
                    .singleOrError()
                    .map { response -> getBundleFromPreKeyResponse(response, deviceId) }
                    .flatMap { bundles ->
                        return@flatMap try {
                            for (bundle in bundles) {
                                val store = AppDependencies.getLocalStorageSessionStore()
                                val preKeyAddress = SignalProtocolAddress(participant, bundle.deviceId)
                                val sessionBuilder = SessionBuilder(store, preKeyAddress)
                                sessionBuilder.process(bundle)
                            }
                            Single.just(Result.success())
                        } catch (e: InvalidKeyException) {
                            Single.just(Result.failure())
                        } catch (e: UntrustedIdentityException) {
                            Single.just(Result.failure())
                        } catch (e: NoSuchElementException) {
                            Single.just(Result.failure())
                        }
                    }
                    .onErrorReturnItem(Result.retry())
            } else {
                return Single.just(Result.failure())
            }
        }

        private fun getBundleFromPreKeyResponse(
            response: PreKeyResponse,
            deviceId: Int
        ): List<PreKeyBundle> {
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
            private val TAG: String = RefreshRemotePreKeyBundleWorker::class.java.simpleName
        }
    }
}