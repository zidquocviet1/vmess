package com.mqv.vmess.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mqv.vmess.network.model.Chat;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insert(Chat chat);

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

    @Query(" select * from chat" +
            " where chat_conversation_id = :conversationId " +
            " and chat_sender_id != :senderId " +
            " order by chat_timestamp desc")
    Single<List<Chat>> fetchIncomingMessageByConversation(String conversationId, String senderId);

    @Query("select * from chat where chat_status = 'NOT_RECEIVED'")
    Single<List<Chat>> fetchNotReceivedChatList();

    @Query("select chat_sender_id from chat" +
            " where chat_conversation_id = :conversationId" +
            " group by chat_sender_id" +
            " having chat_sender_id != :currentUid and chat_sender_id != 'NULL'")
    Single<List<String>> fetchSenderIdFromChat(String conversationId, String currentUid);

    @Query("select chat_sender_id from chat" +
            " group by chat_sender_id" +
            " having chat_sender_id != :currentUid and chat_sender_id != 'NULL'")
    Single<List<String>> fetchSenderIdFromChat(String currentUid);

    @Query("select * from chat where chat_id = :id")
    Single<Chat> findById(String id);

    @Query("select * from chat\n" +
            "where chat_conversation_id = :conversationId\n" +
            "order by chat_timestamp desc\n" +
            "limit 1")
    Single<Chat> findLastMessage(String conversationId);

    @Update
    Completable update(Chat chat);

    @Update
    Completable update(List<Chat> chats);
}
