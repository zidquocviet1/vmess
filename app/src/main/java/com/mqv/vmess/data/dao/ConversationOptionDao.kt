package com.mqv.vmess.data.dao

import androidx.room.*
import com.mqv.vmess.data.model.ConversationIgnoreOption
import com.mqv.vmess.data.model.ConversationNotificationOption
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

@Dao
interface ConversationOptionDao {
    @Query(
        "SELECT * FROM conversation_notification_option" +
                " WHERE until > :currentTimeMilli" +
                " GROUP BY (conversation_id)"
    )
    fun fetchAllNotificationNonExpired(currentTimeMilli: Long): Flowable<List<ConversationNotificationOption>>

    @Query("SELECT * FROM conversation_notification_option")
    fun fetchAllNotification(): Flowable<List<ConversationNotificationOption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllNotification(notifications: List<ConversationNotificationOption>): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllIgnore(ignores: List<ConversationIgnoreOption>): Completable

    @Query("DELETE FROM conversation_notification_option where conversation_id = :conversationId")
    fun deleteNotification(conversationId: String): Completable

    @Query("DELETE FROM conversation_notification_option")
    fun deleteAll(): Completable
}