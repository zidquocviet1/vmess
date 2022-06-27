package com.mqv.vmess.crypto.storage

import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.state.*
import java.util.*

class LocalStorageSessionStore(
    private val identityStore: LocalIdentityKeyStore,
    private val preKeyStore: LocalPreKeyStore,
    private val sessionStore: LocalSessionStore,
    private val senderKeyStore: LocalSenderKeyStore,
) : VMessProtocolStore {
    override fun getIdentityKeyPair(): IdentityKeyPair = identityStore.identityKeyPair

    override fun getLocalRegistrationId(): Int = identityStore.localRegistrationId

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean =
        identityStore.saveIdentity(address, identityKey)

    override fun isTrustedIdentity(
        address: SignalProtocolAddress,
        identityKey: IdentityKey,
        direction: IdentityKeyStore.Direction
    ): Boolean = identityStore.isTrustedIdentity(address, identityKey, direction)

    override fun getIdentity(address: SignalProtocolAddress): IdentityKey =
        identityStore.getIdentity(address)

    override fun loadPreKey(preKeyId: Int): PreKeyRecord =
        preKeyStore.loadPreKey(preKeyId)

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) =
        preKeyStore.storePreKey(preKeyId, record)

    override fun containsPreKey(preKeyId: Int): Boolean =
        preKeyStore.containsPreKey(preKeyId)

    override fun removePreKey(preKeyId: Int) =
        preKeyStore.removePreKey(preKeyId)

    override fun loadSession(address: SignalProtocolAddress): SessionRecord? =
        sessionStore.loadSession(address)

    override fun loadExistingSessions(addresses: MutableList<SignalProtocolAddress>): MutableList<SessionRecord> =
        sessionStore.loadExistingSessions(addresses)

    override fun getSubDeviceSessions(name: String): MutableList<Int> =
        sessionStore.getSubDeviceSessions(name)

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) =
        sessionStore.storeSession(address, record)

    override fun containsSession(address: SignalProtocolAddress): Boolean =
        sessionStore.containsSession(address)

    override fun deleteSession(address: SignalProtocolAddress) =
        sessionStore.deleteSession(address)

    override fun deleteAllSessions(name: String) =
        sessionStore.deleteAllSessions(name)

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord =
        preKeyStore.loadSignedPreKey(signedPreKeyId)

    override fun loadSignedPreKeys(): MutableList<SignedPreKeyRecord> =
        preKeyStore.loadSignedPreKeys()

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) =
        preKeyStore.storeSignedPreKey(signedPreKeyId, record)

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean =
        preKeyStore.containsSignedPreKey(signedPreKeyId)

    override fun removeSignedPreKey(signedPreKeyId: Int) =
        preKeyStore.removeSignedPreKey(signedPreKeyId)

    override fun storeSenderKey(
        sender: SignalProtocolAddress,
        distributionId: UUID,
        record: SenderKeyRecord
    ) = senderKeyStore.storeSenderKey(sender, distributionId, record)

    override fun loadSenderKey(
        sender: SignalProtocolAddress,
        distributionId: UUID
    ): SenderKeyRecord? = senderKeyStore.loadSenderKey(sender, distributionId)

    override fun clearAll() {
        preKeyStore.removeAll()
        sessionStore.removeAll()
        identityStore.removeAll()
    }
}