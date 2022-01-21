package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Chat;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface ChatRepository {
    Single<List<Chat>> fetchUnreadChatByConversation(String conversationId);

    Observable<ApiResponse<Chat>> fetchChatRemoteById(String id);

    Observable<ApiResponse<List<Chat>>> loadMoreChat(@NonNull String conversationId, int page, int size);

    Observable<ApiResponse<Chat>> sendMessage(@NonNull Chat chat);

    Observable<ApiResponse<Chat>> seenMessage(@NonNull Chat chat);

    Observable<ApiResponse<Chat>> seenWelcomeMessage(@NonNull Chat chat);

    Single<List<Chat>> pagingCachedByConversation(String id, int page, int size);

    Single<Chat> fetchCached(String id);

    Completable saveCached(Chat chat);

    void saveCached(List<Chat> chat);

    void updateCached(Chat chat);

    void updateCached(List<Chat> chats);
}
