package com.mqv.realtimechatapplication.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mqv.realtimechatapplication.network.model.Chat;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ChatDao {
    @Query("SELECT * FROM CHAT" +
            " WHERE chat_conversation_id = :conversationId " +
            "ORDER BY chat_timestamp DESC " +
            "LIMIT :size")
    Flowable<List<Chat>> observe(String conversationId, int size);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(List<Chat> chats);

    @Query(" select * from chat" +
           " where chat_conversation_id = :conversationId" +
           " order by chat_timestamp desc" +
           " limit :size")
    Single<List<Chat>> fetchChatByConversation(String conversationId, int size);

    @Query(" select * from chat " +
           " where chat_conversation_id = :conversationId " +
           " order by chat_timestamp desc" +
           " limit :size offset (:size * :page)")
    Single<List<Chat>> fetchChatByConversation(String conversationId, int page, int size);

    @Query(" select * from chat" +
           " where chat_conversation_id = :conversationId " +
                    "and chat_sender_id != :senderId " +
                    "and (chat_status = 'RECEIVED' or chat_status = 'NOT_RECEIVED')" +
           " order by chat_timestamp desc")
    Single<List<Chat>> fetchUnreadChatByConversation(String conversationId, String senderId);

    @Query("select * from chat")
    Single<List<Chat>> fetchNotReceivedChatList();

    @Query("select * from chat where chat_id = :id")
    Single<Chat> findById(String id);

    @Update
    Completable update(Chat chat);

    @Update
    Completable update(List<Chat> chats);
}
