package com.mqv.vmess.crypto.storage

import androidx.annotation.WorkerThread
import com.mqv.vmess.data.MyDatabase
import com.mqv.vmess.data.dao.SenderKeyDao
import com.mqv.vmess.data.model.SenderKeyModel
import com.mqv.vmess.util.DateTimeHelper.toLong
import org.apache.commons.codec.binary.Base64
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.groups.state.SenderKeyStore
import java.time.LocalDateTime
import java.util.UUID

class LocalSenderKeyStore(database: MyDatabase) : DatabaseSessionInteractor(database),
    SenderKeyStore {
    @WorkerThread
    override fun storeSenderKey(
        sender: SignalProtocolAddress,
        distributionId: UUID,
        record: SenderKeyRecord
    ) =
        senderKeyDao.saveSenderKey(record.toSenderKeyModel(sender, distributionId))
            .internalExecute()

    @WorkerThread
    override fun loadSenderKey(
        sender: SignalProtocolAddress,
        distributionId: UUID
    ): SenderKeyRecord? =
        senderKeyDao.getSenderKey(sender.name, distributionId.toString()).internalGet()
            ?.toSenderKeyRecord()


    private fun SenderKeyModel.toSenderKeyRecord(): SenderKeyRecord {
        return SenderKeyRecord(Base64.decodeBase64(this.record))
    }

    private fun SenderKeyRecord.toSenderKeyModel(
        sender: SignalProtocolAddress,
        distributionId: UUID
    ): SenderKeyModel {
        return SenderKeyModel(
            0,
            sender.deviceId,
            sender.name,
            Base64.encodeBase64String(this.serialize()),
            distributionId.toString(),
            LocalDateTime.now().toLong()
        )
    }
}