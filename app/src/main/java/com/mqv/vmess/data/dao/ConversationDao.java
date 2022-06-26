package com.mqv.vmess.data.dao;

import static androidx.room.OnConflictStrategy.IGNORE;
import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.annotation.WorkerThread;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.Conversation;
import com.mqv.vmess.network.model.ConversationGroup;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.network.model.type.ConversationStatusType;
import com.mqv.vmess.network.model.type.ConversationType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public abstract class ConversationDao {
    @Query(" select * from conversation\n" +
           " inner join chat on chat_conversation_id = conversation_id and conversation_status = :statusType\n" +
           " group by conversation_id\n" +
           " order by max(chat_timestamp) desc\n" +
           " limit :limit")
    public abstract Flowable<Map<Conversation, Chat>> observeUnreadConversation(ConversationStatusType statusType, int limit);

    @Query("select * from conversation\n" +
            "inner join chat on chat_conversation_id = conversation_id and conversation_status = :statusType\n" +
            "group by conversation_id\n" +
            "order by max(chat_timestamp) desc\n" +
            "limit :size")
    public abstract Flowable<Map<Conversation, Chat>> conversationAndLastChat(ConversationStatusType statusType, int size);

    @Query("select * from conversation\n" +
            "inner join chat " +
            "on chat_conversation_id = conversation_id and conversation_status = :statusType and conversation_id = :conversationId\n" +
            "group by conversation_id\n" +
            "order by max(chat_timestamp) desc")
    public abstract Single<Map<Conversation, Chat>> conversationAndLastChat(String conversationId, ConversationStatusType statusType);

    @Query("select * from conversation\n" +
            "inner join chat " +
            "on chat_conversation_id = conversation_id and conversation_status = :statusType\n" +
            "group by conversation_id\n" +
            "order by max(chat_timestamp) desc\n" +
            "limit :size\n" +
            "offset(:size * :page)")
    public abstract Single<Map<Conversation, Chat>> fetchCachePaging(ConversationStatusType statusType, int page, int size);


    @Query("select conversation_id, conversation_type, conversation_status, conversation_participants_id, `group` from conversation\n" +
            "inner join chat on chat_conversation_id = conversation_id\n" +
            "group by conversation_id\n" +
            "order by max(chat_timestamp) desc\n" +
            "limit :size")
    public abstract Single<List<Conversation>> suggestConversation(int size);

    @Insert
    public abstract Completable save(Conversation data);

    @Query("select * from conversation " +
           "join chat on chat_conversation_id = conversation_id " +
           "where conversation_id = :id " +
           "order by chat_timestamp desc " +
           "limit 40")
    public abstract Map<Conversation, List<Chat>> conversationAndChat(String id);

    @Query("select * from conversation\n" +
           "inner join chat " +
           "on chat_conversation_id = conversation_id and conversation_id = :conversationId\n" +
           "group by conversation_id\n" +
           "order by max(chat_timestamp) desc")
    public abstract Map<Conversation, Chat> conversationAndLastChat(String conversationId);

    @Insert(onConflict = IGNORE)
    abstract long saveIfNotExists(Conversation data);

    @Insert(onConflict = REPLACE)
    abstract void saveListChat(List<Chat> chats);

    @Update
    abstract void privateUpdate(Conversation conversation);

    @Query(" UPDATE conversation" +
           " SET `conversation_participants_id` = :participants,`group` = :group,`conversation_type` = :type,`conversation_status` = :status" +
           " WHERE `conversation_id` = :id")
    abstract void privateUpdate(String id, List<User> participants, ConversationGroup group, ConversationType type, ConversationStatusType status);

    @Transaction
    public void saveConversationList(List<Conversation> data) {
        saveConversationAndChat(data, true);
    }

    @Transaction
    public void saveConversationListWithoutNotify(List<Conversation> data) {
        saveConversationAndChat(data, false);
    }

    private void saveConversationAndChat(List<Conversation> data, boolean isNotify) {
        data.forEach(c -> {
            long result = saveIfNotExists(c);

            // Result of insertion progress: -1 mean exists conversation and other insert success
            if (result != -1) {
                saveListChat(c.getChats());
            } else {
                Map<Conversation, Chat> conversationMapper   = conversationAndLastChat(c.getId());
                List<Chat>              freshChat            = c.getChats();
                Chat                    lastCacheChat        = Objects.requireNonNull(conversationMapper.get(c));
                Optional<Chat>          presenceChatOptional = freshChat.stream()
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
                    Chat       presenceChat     = presenceChatOptional.get();
                    int        index            = freshChat.indexOf(presenceChat) + 1;
                    List<Chat> shouldInsertList = freshChat.subList(index, freshChat.size());

                    saveListChat(shouldInsertList);
                } else {
                    saveListChat(freshChat);
                }

                // update conversation [GROUP, PARTICIPANTS, STATUS]
                if (isNotify) {
                    privateUpdate(c);
                } else {
                    privateUpdate(c.getId(), c.getParticipants(), c.getGroup(), c.getType(), c.getStatus());
                }
            }
        });
    }

    @Query(" select conversation_id, conversation_participants_id, conversation_status, conversation_type, conversation_creation_time \n" +
           " from conversation \n" +
           " inner join chat \n" +
           " on conversation_id = chat_conversation_id \n" +
           " where conversation_status = :status\n" +
           " group by conversation_id \n" +
           " order by max(chat.chat_timestamp) desc\n" +
           " limit :size" +
           " offset :page")
    public abstract Single<List<Conversation>> fetchAllByStatus(ConversationStatusType status, int page, int size);

    @Query(" select * from conversation co \n" +
           " join chat ch on ch.chat_conversation_id = co.conversation_id" +
           " where conversation_id = :id" +
           " order by ch.chat_timestamp desc" +
           " limit 40")
    public abstract Single<Map<Conversation, List<Chat>>> fetchById(String id);

    @Query("select * from conversation")
    abstract List<Conversation> fetchAll();

    @Query("select * from conversation")
    public abstract Single<List<Conversation>> fetchAllAsync();

    @Update
    public abstract Completable update(Conversation conversation);

    @Query("delete from Conversation where conversation_id = :id")
    abstract void delete(String id);

    @Delete
    abstract void deleteWithoutNIO(Conversation conversation);

    @Query("delete from Conversation")
    public abstract Completable deleteAll();

    @Query("delete from Conversation where conversation_id not in (:conversationListId) and conversation_status = :status")
    public abstract Completable deleteAll(List<String> conversationListId, ConversationStatusType status);

    @Delete
    public abstract Completable delete(Conversation conversation);

    @Query("select exists(select * from conversation where conversation_id = :conversationId)")
    public abstract Single<Boolean> isExists(String conversationId);

    @Transaction
    public void deleteNormalByParticipantId(String userId, String otherUserId) {
        List<Conversation> conversations = fetchAll().stream()
                                                     .filter(c -> c.getType() == ConversationType.NORMAL)
                                                     .collect(Collectors.toList());

        conversations.forEach(c -> {
            List<String> listUserId = c.getParticipants()
                                       .stream()
                                       .map(User::getUid)
                                       .collect(Collectors.toList());

            if (listUserId.containsAll(Arrays.asList(userId, otherUserId))) {
                delete(c.getId());
            }
        });
    }

    @Transaction
    public void deleteByParticipantId(String userId, String otherUserId) {
        List<Conversation> conversations = fetchAll();

        conversations.forEach(c -> {
            List<String> listUserId = c.getParticipants()
                                       .stream()
                                       .map(User::getUid)
                                       .collect(Collectors.toList());

            if (listUserId.containsAll(Arrays.asList(userId, otherUserId)) && c.getType() == ConversationType.NORMAL) {
                deleteWithoutNIO(c);
            }
        });
    }

    public Completable markConversationAsInbox(Conversation conversation) {
        return markConversationStatusChange(conversation, ConversationStatusType.INBOX);
    }

    public Completable markConversationAsArchived(Conversation conversation) {
        return markConversationStatusChange(conversation, ConversationStatusType.ARCHIVED);
    }

    public Completable markConversationAsRequest(Conversation conversation) {
        return markConversationStatusChange(conversation, ConversationStatusType.REQUEST);
    }

    public Completable markConversationStatusChange(Conversation conversation, ConversationStatusType status) {
        conversation.setStatus(status);
        return update(conversation);
    }

    @WorkerThread
    public Conversation getEncryptionConversation(String currentUser, String userId) throws IllegalStateException {
        return fetchAll().stream()
                         .filter(c -> c.getGroup() == null)
                         .filter(c -> c.getEncrypted() != null && c.getEncrypted() &&
                                 c.getParticipants()
                                         .stream()
                                         .map(User::getUid)
                                         .collect(Collectors.toList())
                                         .containsAll(List.of(currentUser, userId)))
                         .findFirst()
                         .orElseThrow(IllegalStateException::new);
    }

    @WorkerThread
    @Query("SELECT conversation_participants_id FROM conversation WHERE conversation_id = :conversationId")
    public abstract Single<String> getParticipantByConversationId(String conversationId);

    @Query("SELECT EXISTS(SELECT * FROM conversation WHERE is_encrypted = 1 AND conversation_id = :conversationId)")
    public abstract Single<Boolean> isEncryptionConversation(String conversationId);
}
