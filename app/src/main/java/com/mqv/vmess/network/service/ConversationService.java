package com.mqv.vmess.network.service;

import static com.mqv.vmess.util.Const.AUTHORIZATION;

import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.model.Conversation;
import com.mqv.vmess.network.model.ConversationOption;
import com.mqv.vmess.network.model.type.ConversationStatusType;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import okhttp3.MultipartBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
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

    @Multipart
    @PUT("conversation/{conversation-id}/group/thumbnail")
    Observable<ApiResponse<Conversation>> changeConversationGroupThumbnail(@Header(AUTHORIZATION) String token,
                                                                           @Path("conversation-id") String conversationId,
                                                                           @Part MultipartBody.Part image);

    @PUT("conversation/{conversation-id}/group/add-member/{id}")
    Observable<ApiResponse<Conversation>> addConversationGroupMember(@Header(AUTHORIZATION) String token,
                                                                     @Path("conversation-id") String conversationId,
                                                                     @Path("id") String memberId);

    @PUT("conversation/{conversation-id}/group/remove-member/{id}")
    Observable<ApiResponse<Conversation>> removeGroupMember(@Header(AUTHORIZATION) String token,
                                                            @Path("conversation-id") String conversationId,
                                                            @Path("id") String memberId);

    @PUT("conversation/{conversation-id}/group/leave")
    Observable<ApiResponse<Conversation>> leaveGroup(@Header(AUTHORIZATION) String token,
                                                     @Path("conversation-id") String conversationId);

    @GET("conversation/mute-notification")
    Observable<ApiResponse<List<ConversationOption>>> getAllMuteNotification(@Header(AUTHORIZATION) String token);

    @PUT("conversation/mute")
    Observable<ApiResponse<ConversationOption>> mute(@Header(AUTHORIZATION) String token,
                                                     @Query("conversation-id") String conversationId,
                                                     @Query("until") long until);

    @PUT("conversation/unmute")
    Observable<Boolean> unmute(@Header(AUTHORIZATION) String token,
                               @Query("conversation-id") String conversationId);

    @POST("conversation/create/encryption")
    @FormUrlEncoded
    Observable<ApiResponse<Conversation>> createEncryptionConversation(@Header(AUTHORIZATION) String token,
                                                                       @Field("userId") String userId);
}
