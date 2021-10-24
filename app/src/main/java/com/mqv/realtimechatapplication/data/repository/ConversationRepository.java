package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

public interface ConversationRepository {
    Observable<ApiResponse<List<Conversation>>> fetchByUid(ConversationStatusType type);

    Observable<List<Conversation>> fetchByUidNBR(ConversationStatusType type);

    Observable<ApiResponse<Chat>> sendMessage(@NonNull Chat chat);

    Completable updateConversation(Conversation conversation);
}
