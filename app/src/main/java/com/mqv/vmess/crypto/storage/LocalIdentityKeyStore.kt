package com.mqv.vmess.crypto.storage

import com.mqv.vmess.data.MyDatabase
import com.mqv.vmess.data.model.IdentityKeyModel
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.util.DateTimeHelper.toLong
import org.apache.commons.codec.binary.Base64
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.state.IdentityKeyStore
import java.io.IOException
import java.time.LocalDateTime

class LocalIdentityKeyStore(
    database: MyDatabase,
    private val userId: String,
) : DatabaseSessionInteractor(database), IdentityKeyStore {
    override fun getIdentityKeyPair(): IdentityKeyPair {
        val identityKey = AppDependencies.getAppPreferences().identityKey
        val identityPrivateKey = AppDependencies.getAppPreferences().identityPrivateKey

        if (identityKey.isPresent && identityPrivateKey.isPresent) {
            return IdentityKeyPair(
                IdentityKey(Base64.decodeBase64(identityKey.get())),
                Curve.decodePrivatePoint(
                    Base64.decodeBase64(identityPrivateKey.get())
                )
            )
        } else {
            throw IOException("No such IdentityKeyPair!")
        }
    }

    override fun getLocalRegistrationId(): Int =
        AppDependencies.getAppPreferences().registrationId

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        val identityKeyModel = identityKeyDao.getIdentityKeyModel(address.name).internalGet()

        identityKeyDao.saveIdentity(
            IdentityKeyModel(
                0,
                address.name,
                Base64.encodeBase64String(identityKey.serialize()),
                LocalDateTime.now().toLong()
            )
        )

        return identityKeyModel != null
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean {
        val isSelf = address.name == userId
        if (isSelf) {
            return identityKey == identityKeyPair.publicKey
        }

        return when (direction) {
            IdentityKeyStore.Direction.SENDING -> {
                val preKeyModel =
                    identityKeyDao.getIdentityKeyModel(address.name).internalGet() ?: return true
                val localIdentityKey = IdentityKey(Base64.decodeBase64(preKeyModel.identityKey))

                if (identityKey != localIdentityKey) {
                    return false
                }

                return true
            }
            IdentityKeyStore.Direction.RECEIVING -> true
            else -> throw IOException("Unknown direction: $direction")
        }
    }

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey =
        IdentityKey(
            Base64.decodeBase64(
                identityKeyDao.getIdentityKey(address.name).internalGet()
            )
        )
}

