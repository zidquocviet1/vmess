package com.mqv.realtimechatapplication.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mqv.realtimechatapplication.network.model.Chat;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface ChatDao {
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

    @Update
    Completable update(Chat chat);

    @Update
    Completable update(List<Chat> chats);
}
