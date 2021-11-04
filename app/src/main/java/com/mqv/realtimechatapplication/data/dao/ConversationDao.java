package com.mqv.realtimechatapplication.data.dao;

import static androidx.room.OnConflictStrategy.IGNORE;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;
import com.mqv.realtimechatapplication.util.Retriever;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public abstract class ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract Completable saveAll(List<Conversation> data);

    @Insert
    public abstract Completable save(Conversation data);

    @Query("select * from conversation " +
           "join chat on chat_conversation_id = conversation_id " +
           "where conversation_id = :id " +
           "order by chat_timestamp desc " +
           "limit 40")
    abstract Map<Conversation, List<Chat>> conversationAndChat(String id);

    @Insert(onConflict = IGNORE)
    abstract long saveIfNotExists(Conversation data);

    @Insert
    abstract void saveListChat(List<Chat> chats);

    @Transaction
    public void saveConversationList(List<Conversation> data) {
        data.forEach(c -> {
            long result = saveIfNotExists(c);

            // Result of insertion progress: -1 mean exists conversation and other insert success
            if (result != -1) {
                saveListChat(c.getChats());
            } else {
                Map<Conversation, List<Chat>> conversationMapper = conversationAndChat(c.getId());

                List<Chat> freshChat = c.getChats();
                List<Chat> cacheChat = Retriever.getOrDefault(conversationMapper.get(c), new ArrayList<>());

                Collections.reverse(cacheChat);

                // Find the last cache chat appear in fresh chat
                Chat lastCacheChat = cacheChat.get(cacheChat.size() - 1);

                Optional<Chat> presenceChatOptional =
                        freshChat.stream()
                                .filter(ch -> ch.getId().equals(lastCacheChat.getId()))
                                .findFirst();

                if (presenceChatOptional.isPresent()) {
                    /*
                     * Example:
                     * Cache Chat: [A, B, C, D, E]
                     * Fresh Chat: [C, D, E, F, G, H]
                     * So the E is the presence last chat in cache.
                     * We need to add the next sublist to cache [F, G, H]
                     * */
                    Chat presenceChat = presenceChatOptional.get();

                    int index = freshChat.indexOf(presenceChat);

                    List<Chat> shouldInsertList = freshChat.subList(index + 1, freshChat.size());

                    saveListChat(shouldInsertList);
                } else {
                    saveListChat(freshChat);
                }
            }
        });
    }

    @Query("select * from Conversation where conversation_status = :status")
    public abstract Single<List<Conversation>> fetchAllByStatus(ConversationStatusType status);

    @Query("select * from conversation co \n" +
            "join chat ch on ch.chat_conversation_id = co.conversation_id \n" +
            "where co.conversation_status = :status")
    public abstract Map<Conversation, List<Chat>> fetchAllRelationStatus(ConversationStatusType status);

    @Query(" select * from conversation co \n" +
           " join chat ch on ch.chat_conversation_id = co.conversation_id" +
           " where conversation_id = :id" +
           " order by ch.chat_timestamp desc" +
           " limit 40")
    public abstract Single<Map<Conversation, List<Chat>>> fetchById(String id);

    @Update
    public abstract Completable update(Conversation conversation);

    @Query("delete from Conversation")
    public abstract Completable deleteAll();

    @Query("delete from Conversation where conversation_id not in (:conversationListId)")
    public abstract Completable deleteAll(List<String> conversationListId);
}
