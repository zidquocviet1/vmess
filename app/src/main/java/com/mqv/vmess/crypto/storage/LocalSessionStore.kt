package com.mqv.vmess.crypto.storage

import com.mqv.vmess.data.MyDatabase
import androidx.annotation.WorkerThread
import com.mqv.vmess.data.model.SessionModel
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SessionStore
import org.apache.commons.codec.binary.Base64
import org.signal.libsignal.protocol.NoSessionException
import org.signal.libsignal.protocol.message.CiphertextMessage
import java.util.*
import kotlin.streams.toList

class LocalSessionStore(database: MyDatabase, private val userId: String) :
    DatabaseSessionInteractor(database), SessionStore {

    @WorkerThread
    override fun loadSession(address: SignalProtocolAddress): SessionRecord? {
        val record = sessionDao.getSessionRecord(userId, address.name, address.deviceId).internalGet()
            ?.let { encodedRecord ->
                SessionRecord(Base64.decodeBase64(encodedRecord))
            }
        return record
    }


    @WorkerThread
    override fun loadExistingSessions(addresses: MutableList<SignalProtocolAddress>): MutableList<SessionRecord> {
        val records = LinkedList<SessionRecord?>()

        for (address in addresses) {
            records.add(loadSession(address))
        }

        if (records.size != addresses.size) {
            throw NoSessionException("Mismatch, asked for ${addresses.size} but found ${records.size}")
        }

        if (records.stream().anyMatch(Objects::isNull)) {
            throw NoSessionException("Failed to find one or more sessions.")
        }

        return records.stream().map { nonNullRecord -> nonNullRecord!! }.toList().toMutableList()
    }

    @WorkerThread
    override fun getSubDeviceSessions(name: String): MutableList<Int> =
        sessionDao.getSubDevicesSession(userId, name, 1).requireGet().toMutableList()

    @WorkerThread
    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        SessionModel(
            0,
            userId,
            address.name,
            address.deviceId,
            Base64.encodeBase64String(record.serialize())
        ).let { model ->
            sessionDao.saveSession(model).internalExecute()
        }
    }

    @WorkerThread
    override fun containsSession(address: SignalProtocolAddress): Boolean {
        val record = loadSession(address)

        return record != null &&
                record.hasSenderChain() &&
                record.sessionVersion == CiphertextMessage.CURRENT_VERSION
    }

    @WorkerThread
    override fun deleteSession(address: SignalProtocolAddress) =
        sessionDao.deleteSession(userId, address.name, address.deviceId).internalExecute()

    @WorkerThread
    override fun deleteAllSessions(name: String) =
        sessionDao.deleteAllSessionByRemoteAddress(userId, name).internalExecute()

    @WorkerThread
    fun removeAll() {
        sessionDao.removeAll().internalExecute()
    }
}