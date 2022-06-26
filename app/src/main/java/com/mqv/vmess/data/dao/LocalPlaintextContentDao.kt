package com.mqv.vmess.data.dao

import androidx.room.*
import com.mqv.vmess.data.model.LocalPlaintextContentModel
import com.mqv.vmess.network.model.type.ConversationStatusType
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

@Dao
interface LocalPlaintextContentDao {
    @Query("SELECT * FROM local_plaintext_content WHERE conversationId = :conversationId")
    fun fetchAll(conversationId: String): Flowable<List<LocalPlaintextContentModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(model: LocalPlaintextContentModel): Completable

    @Query("DELETE FROM local_plaintext_content")
    fun deleteAll(): Completable

    @MapInfo(keyColumn = "conversation_id", valueTable = "lpc")
    @Query("select conversation_id, lpc.conversationId, lpc.messageId, lpc.content\n" +
            "from conversation\n" +
            "inner join chat on chat_conversation_id = conversation_id\n" +
            "inner join local_plaintext_content lpc on messageId = chat_id\n" +
            "where is_encrypted = 1 AND conversation_status = :status\n" +
            "order by chat_timestamp desc\n" +
            "limit 1")
    fun getLastMessageForEncryptedConversation(status: ConversationStatusType): Single<Map<String, LocalPlaintextContentModel>>
}