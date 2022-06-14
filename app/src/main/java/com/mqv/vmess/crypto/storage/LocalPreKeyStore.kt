package com.mqv.vmess.crypto.storage

import androidx.annotation.WorkerThread
import com.mqv.vmess.data.MyDatabase
import com.mqv.vmess.data.model.PreKeyModel
import com.mqv.vmess.data.model.SignedPreKeyModel
import com.mqv.vmess.util.DateTimeHelper.toLong
import org.apache.commons.codec.binary.Base64
import org.signal.libsignal.protocol.InvalidKeyIdException
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyStore
import java.time.LocalDateTime
import kotlin.streams.toList

class LocalPreKeyStore(
    database: MyDatabase,
    private val userId: String,
) : DatabaseSessionInteractor(database), PreKeyStore, SignedPreKeyStore {
    // region PreKey Session
    @WorkerThread
    override fun loadPreKey(preKeyId: Int): PreKeyRecord =
        preKeyDao.getPreKey(preKeyId, userId).internalGet()?.toPreKeyRecord()
            ?: throw InvalidKeyIdException("Key id is not valid")

    @WorkerThread
    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        val publicKey = Base64.encodeBase64String(record.keyPair.publicKey.serialize())
        val privateKey = Base64.encodeBase64String(record.keyPair.privateKey.serialize())
        val model =
            PreKeyModel(0, userId, preKeyId, publicKey, privateKey, LocalDateTime.now().toLong())

        preKeyDao.savePreKey(model).internalExecute()
    }

    @WorkerThread
    override fun containsPreKey(preKeyId: Int): Boolean =
        preKeyDao.containsPreKey(preKeyId, userId).internalGet() ?: false

    @WorkerThread
    override fun removePreKey(preKeyId: Int) =
        preKeyDao.removePreKey(preKeyId, userId).internalExecute()

    // endregion

    // region SignedPreKey Session

    @WorkerThread
    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord =
        signedPreKeyDao.getSignedPreKey(signedPreKeyId, userId).internalGet()
            ?.toSignedPreKeyRecord() ?: throw InvalidKeyIdException("Signed PreKey id not valid")

    @WorkerThread
    override fun loadSignedPreKeys(): MutableList<SignedPreKeyRecord> =
        signedPreKeyDao.getAllSignedPreKeys(userId)
            .internalGet()
            ?.stream()
            ?.map { model -> model.toSignedPreKeyRecord() }
            ?.toList()
            ?.toMutableList() ?: mutableListOf()

    @WorkerThread
    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        val publicKey = Base64.encodeBase64String(record.keyPair.publicKey.serialize())
        val privateKey = Base64.encodeBase64String(record.keyPair.privateKey.serialize())
        val signature = Base64.encodeBase64String(record.signature)
        val model =
            SignedPreKeyModel(
                0,
                userId,
                signedPreKeyId,
                publicKey,
                privateKey,
                signature,
                LocalDateTime.now().toLong()
            )

        signedPreKeyDao.saveSignedPreKey(model).internalExecute()
    }

    @WorkerThread
    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean =
        signedPreKeyDao.containsSignedPreKey(signedPreKeyId, userId).internalGet() ?: false

    @WorkerThread
    override fun removeSignedPreKey(signedPreKeyId: Int) =
        signedPreKeyDao.removeSignedPreKey(signedPreKeyId, userId).internalExecute()

    // endregion

    private fun PreKeyModel.toPreKeyRecord(): PreKeyRecord {
        val publicKey = Curve.decodePoint(Base64.decodeBase64(this.publicKey), 0)
        val privateKey = Curve.decodePrivatePoint(Base64.decodeBase64(this.privateKey))

        return PreKeyRecord(this.keyId, ECKeyPair(publicKey, privateKey))
    }

    private fun SignedPreKeyModel.toSignedPreKeyRecord(): SignedPreKeyRecord {
        val publicKey = Curve.decodePoint(Base64.decodeBase64(this.publicKey), 0)
        val privateKey = Curve.decodePrivatePoint(Base64.decodeBase64(this.privateKey))

        return SignedPreKeyRecord(
            this.keyId,
            this.timestamp,
            ECKeyPair(publicKey, privateKey),
            Base64.decodeBase64(this.signature)
        )
    }
}