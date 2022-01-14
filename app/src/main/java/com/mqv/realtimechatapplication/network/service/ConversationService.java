package com.mqv.realtimechatapplication.network.service;

import static com.mqv.realtimechatapplication.util.Const.AUTHORIZATION;

import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ConversationService {
    @GET("conversation")
    Observable<ApiResponse<List<Conversation>>> fetchConversation(@Header(AUTHORIZATION) String token,
                                                                  @Query("status") ConversationStatusType type,
                                                                  @Query("page") int page,
                                                                  @Query("size") int size);

    @GET("conversation/find_by_participant_id")
    Observable<ApiResponse<Conversation>> findNormalByParticipantId(@Header(AUTHORIZATION) String token,
                                                                    @Query("id") String otherId);

    @PUT("conversation/change_status/{id}")
    Observable<ApiResponse<Conversation>> requestChangeConversationStatus(@Header(AUTHORIZATION) String token,
                                                                          @Path("id") String conversationId,
                                                                          @Query("status") int ordinal);

    @GET("conversation/find_by_id")
    Single<ApiResponse<Conversation>> findById(@Header(AUTHORIZATION) String token,
                                               @Query("id") String conversationId);

    @DELETE("conversation/{id}")
    Observable<ApiResponse<String>> deleteConversationChat(@Header(AUTHORIZATION) String token,
                                                           @Path("id") String conversationId);
}
