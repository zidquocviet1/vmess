package com.mqv.vmess.crypto

import com.mqv.vmess.crypto.storage.PreKeyMetadataStore
import com.mqv.vmess.util.Logging
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.InvalidKeyIdException
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignalProtocolStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.Medium
import java.util.*
import java.util.concurrent.TimeUnit

object PreKeyUtil {
    private val TAG: String = PreKeyUtil::class.java.simpleName
    private val ARCHIVE_AGE = TimeUnit.DAYS.toMillis(30)
    private const val BATCH_SIZE = 100

    @Synchronized
    fun generateAndStoreOneTimePreKeys(
        protocolStore: SignalProtocolStore,
        metadataStore: PreKeyMetadataStore
    ): List<PreKeyRecord> {
        Logging.debug(TAG, "Generating one-time preKeys...")

        val records: MutableList<PreKeyRecord> = LinkedList()
        val preKeyIdOffset: Int = metadataStore.nextOneTimePreKeyId

        for (i in 0 until BATCH_SIZE) {
            val preKeyId = (preKeyIdOffset + i) % Medium.MAX_VALUE
            val keyPair = Curve.generateKeyPair()
            val record = PreKeyRecord(preKeyId, keyPair)

            protocolStore.storePreKey(preKeyId, record)
            records.add(record)
        }

        metadataStore.nextOneTimePreKeyId = (preKeyIdOffset + BATCH_SIZE + 1) % Medium.MAX_VALUE

        return records
    }

    @Synchronized
    fun generateAndStoreSignedPreKey(
        protocolStore: SignalProtocolStore,
        metadataStore: PreKeyMetadataStore,
        setAsActive: Boolean
    ): SignedPreKeyRecord {
        Logging.debug(TAG, "Generating signed preKeys...")

        return try {
            val signedPreKeyId: Int = metadataStore.nextSignedPreKeyId
            val keyPair = Curve.generateKeyPair()
            val signature = Curve.calculateSignature(
                protocolStore.identityKeyPair.privateKey,
                keyPair.publicKey.serialize()
            )
            val record =
                SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), keyPair, signature)

            protocolStore.storeSignedPreKey(signedPreKeyId, record)
            metadataStore.nextSignedPreKeyId = (signedPreKeyId + 1) % Medium.MAX_VALUE

            if (setAsActive) {
                metadataStore.activeSignedPreKeyId = signedPreKeyId
            }
            record
        } catch (e: InvalidKeyException) {
            throw AssertionError(e)
        }
    }

    /**
     * Finds all of the signed preKeys that are older than the archive age, and archive all but the youngest of those.
     */
    @Synchronized
    fun cleanSignedPreKeys(protocolStore: SignalProtocolStore, metadataStore: PreKeyMetadataStore) {
        Logging.debug(TAG, "Cleaning signed preKeys...")

        val activeSignedPreKeyId: Int = metadataStore.activeSignedPreKeyId
        if (activeSignedPreKeyId < 0) {
            return
        }
        try {
            val now = System.currentTimeMillis()
            val currentRecord = protocolStore.loadSignedPreKey(activeSignedPreKeyId)
            val allRecords = protocolStore.loadSignedPreKeys()
            allRecords.stream()
                .filter { r -> r.id != currentRecord.id }
                .filter { r -> now - r.timestamp > ARCHIVE_AGE }
                .sorted(Comparator.comparingLong { obj: SignedPreKeyRecord -> obj.timestamp }
                    .reversed())
                .skip(1)
                .forEach { record: SignedPreKeyRecord ->
                    Logging.debug(
                        TAG,
                        "Removing signed preKey record: " + record.id + " with timestamp: " + record.timestamp
                    )
                    protocolStore.removeSignedPreKey(record.id)
                }
        } catch (e: InvalidKeyIdException) {
            Logging.debug(TAG, e.message)
        }
    }
}