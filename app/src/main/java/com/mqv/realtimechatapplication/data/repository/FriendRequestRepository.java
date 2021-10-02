package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.FriendRequest;
import com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus;

import java.util.List;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;

public interface FriendRequestRepository {
    void getAllPendingRequest(FirebaseUser user,
                              Consumer<Observable<ApiResponse<List<FriendRequest>>>> onAuthSuccess,
                              Consumer<Exception> onAuthFail);

    void findFriendRequestStatusByReceiverId(FirebaseUser user,
                                             @NonNull String receiverId,
                                             Consumer<Observable<ApiResponse<FriendRequestStatus>>> onAuthSuccess,
                                             Consumer<Exception> onAuthFail);

    void responseFriendRequest(FirebaseUser user,
                               @Body FriendRequest request,
                               Consumer<Observable<ApiResponse<Boolean>>> onAuthSuccess,
                               Consumer<Exception> onAuthFail);

    void requestConnect(FirebaseUser user,
                        @Body FriendRequest request,
                        Consumer<Observable<ApiResponse<Boolean>>> onAuthSuccess,
                        Consumer<Exception> onAuthFail);

    Observable<ApiResponse<List<String>>> getFriendListId(String token);
}
