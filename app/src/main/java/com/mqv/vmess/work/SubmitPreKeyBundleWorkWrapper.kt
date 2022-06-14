package com.mqv.vmess.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import androidx.work.rxjava3.RxWorker
import com.google.firebase.auth.FirebaseAuth
import com.mqv.vmess.crypto.PreKeyUtil
import com.mqv.vmess.crypto.storage.PreKeyMetadataStore
import com.mqv.vmess.crypto.storage.PreKeyMetadataStoreImpl
import com.mqv.vmess.data.repository.impl.KeyRepositoryImpl
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.model.PreKeyEntity
import com.mqv.vmess.network.model.PreKeyStateEntity
import com.mqv.vmess.network.model.SignedPreKeyEntity
import com.mqv.vmess.util.Logging
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Single
import org.apache.commons.codec.binary.Base64
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.state.SignalProtocolStore
import org.signal.libsignal.protocol.util.KeyHelper
import java.util.*
import java.util.concurrent.TimeUnit

class SubmitPreKeyBundleWorkWrapper(context: Context) : BaseWorker(context) {
    override fun retrieveConstraint(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    override fun isUniqueWork(): Boolean = false

    override fun createRequest(): WorkRequest =
        OneTimeWorkRequest.Builder(SubmitPreKeyBundleWorker::class.java)
            .setConstraints(retrieveConstraint())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 3, TimeUnit.SECONDS)
            .addTag("SubmitSignedPreKey")
            .build()

    companion object {
        @JvmStatic
        fun enqueueIfNeeded(context: Context) {
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                Logging.debug(
                    SubmitPreKeyBundleWorker.TAG,
                    "No user registered for submit signed preKeys"
                )
            } else {
                AppDependencies.getAppPreferences().setAccountId(user.uid)
                WorkDependency.enqueue(SubmitPreKeyBundleWorkWrapper(context))
            }
        }
    }

    @HiltWorker
    class SubmitPreKeyBundleWorker @AssistedInject constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val repository: KeyRepositoryImpl,
    ) : RxWorker(appContext, workerParams) {
        override fun createWork(): Single<Result> {
            generateIdentityKeyIfNeeded()

            val metadataStore = PreKeyMetadataStoreImpl()
            if (metadataStore.isSignedPreKeyRegistered) {
                Logging.debug(TAG, "The user already registered signed preKey...")

                return Single.just(Result.success())
            }

            return makeRegisterPreKey(AppDependencies.getLocalStorageSessionStore(), metadataStore)
        }

        private fun makeRegisterPreKey(
            protocolStore: SignalProtocolStore,
            metadataStore: PreKeyMetadataStore
        ): Single<Result> {
            val preKeys = PreKeyUtil.generateAndStoreOneTimePreKeys(protocolStore, metadataStore)
            val signedPreKeyRecord =
                PreKeyUtil.generateAndStoreSignedPreKey(protocolStore, metadataStore, false)
            val preKeyEntity = LinkedList<PreKeyEntity>()

            try {
                for (preKey in preKeys) {
                    preKeyEntity.add(
                        PreKeyEntity(preKey.id, preKey.keyPair.publicKey)
                    )
                }
            } catch (e: InvalidKeyException) {
                throw AssertionError("Invalid Key when mapping to PreKeyEntity...")
            }

            val registrationId = getRegistrationId()
            val identityKey = protocolStore.identityKeyPair.publicKey
            val signedPreKeyEntity = SignedPreKeyEntity(
                signedPreKeyRecord.id,
                signedPreKeyRecord.keyPair.publicKey,
                signedPreKeyRecord.signature
            )
            val preKeyState = PreKeyStateEntity(identityKey, preKeyEntity, signedPreKeyEntity)

            return if (runAttemptCount < 3) {
                try {
                    Single.just(Result.success())
                        .zipWith(
                            repository.setKeys(preKeyState, registrationId).singleOrError()
                                .doOnError { t ->
                                    Logging.debug(
                                        TAG,
                                        "Submit PreKeyBundle not success because: ${t.message}"
                                    )
                                }) { result, _ ->
                            result
                        }
                        .onErrorReturnItem(Result.retry())
                } catch (e: Exception) {
                    Single.just(Result.retry())
                }
            } else {
                Single.just(Result.failure())
            }
        }

        private fun getRegistrationId(): Int {
            var registrationId = AppDependencies.getAppPreferences().registrationId
            if (registrationId == 0) {
                registrationId = KeyHelper.generateRegistrationId(false)
                AppDependencies.getAppPreferences().registrationId = registrationId
            }
            return registrationId
        }

        private fun generateIdentityKeyIfNeeded() {
            if (AppDependencies.getAppPreferences().isContainIdentityKey) {
                Logging.debug(TAG, "Tried to generate identity key but one was already set")

                return
            }

            Logging.debug(TAG, "Generate a new identity keypair")

            val identityKeyPair = IdentityKeyPair.generate()

            AppDependencies.getAppPreferences()
                .setIdentityKey(Base64.encodeBase64String(identityKeyPair.publicKey.serialize()))
            AppDependencies.getAppPreferences()
                .setIdentityPrivateKey(Base64.encodeBase64String(identityKeyPair.privateKey.serialize()))
        }

        companion object {
            internal val TAG: String = SubmitPreKeyBundleWorker::class.java.simpleName
        }
    }
}