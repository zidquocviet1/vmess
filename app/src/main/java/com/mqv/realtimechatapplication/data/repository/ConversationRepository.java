package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface ConversationRepository {
    Observable<ApiResponse<List<Conversation>>> fetchByUid(ConversationStatusType type, int page, int size);

    Observable<List<Conversation>> fetchByUidNBR(ConversationStatusType type, int page, int size, Runnable onDataChanged);

    Single<List<Conversation>> fetchCached(ConversationStatusType type, int page, int size);

    Observable<ApiResponse<Chat>> sendMessage(@NonNull Chat chat);

    Observable<ApiResponse<Chat>> seenMessage(@NonNull Chat chat);

    Observable<ApiResponse<Boolean>> isServerAlive();

    Observable<ApiResponse<List<Chat>>> loadMoreChat(@NonNull String conversationId, int page, int size);

    Completable saveAll(List<Conversation> conversations);

    Completable deleteAll(List<String> conversationIdList);

    Single<Conversation> fetchCachedById(Conversation conversation);

    Single<List<Chat>> fetchChatByConversation(String id, int page, int size);

    void saveChat(List<Chat> chats);

    void updateChat(Chat chat);
}
