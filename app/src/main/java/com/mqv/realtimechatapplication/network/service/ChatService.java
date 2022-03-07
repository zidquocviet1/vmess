package com.mqv.realtimechatapplication.network.service;

import static com.mqv.realtimechatapplication.util.Const.AUTHORIZATION;

import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Chat;
import com.mqv.realtimechatapplication.util.Const;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ChatService {
    @GET(value = "chat/{id}")
    Observable<ApiResponse<Chat>> fetchById(@Header(Const.AUTHORIZATION) String token,
                                            @Path("id") String id);

    @POST(value = "chat/send")
    Observable<ApiResponse<Chat>> sendMessage(@Header(AUTHORIZATION) String token,
                                              @Body Chat chat);

    @PUT(value = "chat")
    Observable<ApiResponse<Chat>> seenMessage(@Header(AUTHORIZATION) String token,
                                              @Body Chat chat);

    @PUT(value = "chat/seen_welcome_chat")
    Observable<ApiResponse<Chat>> seenWelcomeMessage(@Header(AUTHORIZATION) String token,
                                                     @Body Chat chat);

    @GET(value = "chat/page/{conversationId}")
    Observable<ApiResponse<List<Chat>>> loadMoreChat(@Header(AUTHORIZATION) String token,
                                                     @Path("conversationId") String conversationId,
                                                     @Query("page") int page,
                                                     @Query("size") int size);

    @PUT(value = "chat/mark-received")
    Observable<ApiResponse<Chat>> notifyReceiveMessage(@Header(AUTHORIZATION) String token,
                                                       @Query(value = "id") String messageId);
}
