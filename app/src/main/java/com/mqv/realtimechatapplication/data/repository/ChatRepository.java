package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Chat;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface ChatRepository {
    Flowable<List<Chat>> observeMessages(String conversationId, int size);

    Observable<ApiResponse<Chat>> fetchChatRemoteById(String id);

    Observable<ApiResponse<List<Chat>>> loadMoreChat(@NonNull String conversationId, int page, int size);

    Observable<ApiResponse<Chat>> sendMessage(@NonNull Chat chat);

    Observable<ApiResponse<Chat>> seenMessage(@NonNull Chat chat);

    Observable<ApiResponse<Chat>> seenWelcomeMessage(@NonNull Chat chat);

    Single<List<Chat>> pagingCachedByConversation(String id, int page, int size);

    Single<Chat> fetchCached(String id);

    void saveCached(List<Chat> chat);

    void updateCached(Chat chat);

    void updateCached(List<Chat> chats);
}
