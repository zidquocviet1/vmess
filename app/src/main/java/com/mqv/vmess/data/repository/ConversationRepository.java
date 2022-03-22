package com.mqv.vmess.data.repository;

import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.model.Chat;
import com.mqv.vmess.network.model.Conversation;
import com.mqv.vmess.network.model.type.ConversationStatusType;

import java.io.File;
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

    Single<ApiResponse<Conversation>> findNormalByParticipantId(String participantId);

    Completable save(Conversation conversation);

    Completable saveAll(List<Conversation> freshData, ConversationStatusType type);

    Completable deleteAll();

    Completable delete(Conversation conversation);

    Single<Conversation> fetchCachedById(String conversationId);

    Single<List<Conversation>> fetchAllWithoutMessages();

    Single<Boolean> isExists(String conversationId);

    void deleteConversationChatRemote(Conversation conversation);

    void deleteNormalByParticipantId(String userId, String otherUserId);

    void deleteByParticipantId(String userId, String otherUserId);

    /*
    * Throws: 404, 409, 403
    * */
    Observable<ApiResponse<Conversation>> createGroup(Conversation conversation);

    /////// Conversation changes option
    Completable changeConversationStatus(Conversation conversation);

    Observable<ApiResponse<Conversation>> changeConversationStatusRemote(String conversationId, int ordinal);

    Observable<ApiResponse<Conversation>> changeConversationGroupName(String conversationId, String groupName);

    Observable<ApiResponse<Conversation>> changeConversationGroupThumbnail(String conversationId, File image);

    Observable<ApiResponse<Conversation>> addGroupMember(String conversationId, String memberId);

    Observable<ApiResponse<Conversation>> removeGroupMember(String conversationId, String memberId);

    Observable<ApiResponse<Conversation>> leaveGroup(String conversationId);
}
