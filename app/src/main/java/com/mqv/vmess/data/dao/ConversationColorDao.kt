package com.mqv.vmess.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mqv.vmess.data.model.ConversationColor
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

@Dao
interface ConversationColorDao {
    @Query("SELECT * FROM conversation_color WHERE conversation_id = :conversationId")
    fun fetch(conversationId: String): Flowable<List<ConversationColor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(color: ConversationColor): Completable

    @Query("DELETE FROM conversation_color")
    fun deleteAll(): Completable
}