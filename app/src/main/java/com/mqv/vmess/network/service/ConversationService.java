package com.mqv.vmess.network.service;

import static com.mqv.vmess.util.Const.AUTHORIZATION;

import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.model.Conversation;
import com.mqv.vmess.network.model.type.ConversationStatusType;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ConversationService {
    @GET("conversation")
    Observable<ApiResponse<List<Conversation>>> fetchConversation(@Header(AUTHORIZATION) String token,
                                                                  @Query("status") ConversationStatusType type,
                                                                  @Query("page") int page,
                                                                  @Query("size") int size);

    @GET("conversation/find-by-participant-id")
    Observable<ApiResponse<Conversation>> findNormalByParticipantId(@Header(AUTHORIZATION) String token,
                                                                    @Query("id") String otherId);

    @PUT("conversation/change-status/{id}")
    Observable<ApiResponse<Conversation>> requestChangeConversationStatus(@Header(AUTHORIZATION) String token,
                                                                          @Path("id") String conversationId,
                                                                          @Query("status") int ordinal);

    @GET("conversation/find")
    Single<ApiResponse<Conversation>> findById(@Header(AUTHORIZATION) String token,
                                               @Query("id") String conversationId);

    @DELETE("conversation/{id}")
    Observable<ApiResponse<String>> deleteConversationChat(@Header(AUTHORIZATION) String token,
                                                           @Path("id") String conversationId);

    @POST("conversation/")
    Observable<ApiResponse<Conversation>> createGroup(@Header(AUTHORIZATION) String token,
                                                      @Body Conversation conversation);

    @PUT("conversation/group/edit-name")
    Observable<ApiResponse<Conversation>> changeConversationGroupName(@Header(AUTHORIZATION) String token,
                                                                      @Query("name") String groupName,
                                                                      @Query("id") String conversationId);

    @PUT("conversation/{conversation-id}/group/add-member/{id}")
    Observable<ApiResponse<Conversation>> addConversationGroupMember(@Header(AUTHORIZATION) String token,
                                                                     @Path("conversation-id") String conversationId,
                                                                     @Path("id") String memberId);
}
