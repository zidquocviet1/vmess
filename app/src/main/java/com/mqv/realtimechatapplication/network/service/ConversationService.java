package com.mqv.realtimechatapplication.network.service;

import static com.mqv.realtimechatapplication.util.Const.*;

import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.network.model.Conversation;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface ConversationService {
    @GET("conversation")
    Observable<ApiResponse<List<Conversation>>> fetchConversation(@Header(AUTHORIZATION) String token,
                                                                  @Query("status") ConversationStatusType type,
                                                                  @Query("page") int page,
                                                                  @Query("size") int size);

    @POST("conversation/add_chat")
    Observable<ApiResponse<Chat>> sendMessage(@Header(AUTHORIZATION) String token,
                                              @Header(AUTHORIZER) String authorizer,
                                              @Body Chat chat);

    @PUT("conversation")
    Observable<ApiResponse<Chat>> seenMessage(@Header(AUTHORIZATION) String token,
                                              @Header(AUTHORIZER) String authorizer,
                                              @Body Chat chat);

    @GET("conversation/is_alive")
    Observable<ApiResponse<Boolean>> isServerAlive();

    @GET
    Observable<Map<String, String>> sseConversations(@Url String url,
                                                     @Header(AUTHORIZATION) String token);

    @GET("conversation/chat/{conversationId}")
    Observable<ApiResponse<List<Chat>>> loadMoreChat(@Header(AUTHORIZATION) String token,
                                                     @Path("conversationId") String conversationId,
                                                     @Query("page") int page,
                                                     @Query("size") int size);
}
