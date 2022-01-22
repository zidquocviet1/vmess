package com.mqv.realtimechatapplication.data.repository;

import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface ConversationRepository {
    Flowable<Map<Conversation, Chat>> conversationAndLastChat(ConversationStatusType statusType);

    Single<Map<Conversation, Chat>> conversationAndLastChat(String conversationId, ConversationStatusType statusType);

    Observable<ApiResponse<Conversation>> fetchById(String conversationId);

    Observable<ApiResponse<List<Conversation>>> fetchByUid(ConversationStatusType type, int page, int size);

    Observable<List<Conversation>> fetchByUidNBR(ConversationStatusType type, int page, int size, Runnable onDataChanged);

    Single<List<Conversation>> fetchCached(ConversationStatusType type, int page, int size);

    Completable save(Conversation conversation);

    Completable saveAll(List<Conversation> freshData, ConversationStatusType type);

    Completable deleteAll();

    Completable delete(Conversation conversation);

    void deleteConversationChatRemote(Conversation conversation);

    Single<Conversation> fetchCachedById(String conversationId);

    void deleteNormalByParticipantId(String userId, String otherUserId);

    void deleteByParticipantId(String userId, String otherUserId);

    /////// Conversation changes option
    Completable changeConversationStatus(Conversation conversation);

    Observable<ApiResponse<Conversation>> changeConversationStatusRemote(String conversationId, int ordinal);
}
