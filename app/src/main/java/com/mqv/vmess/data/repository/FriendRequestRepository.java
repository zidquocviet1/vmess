package com.mqv.vmess.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.model.FriendRequest;
import com.mqv.vmess.network.model.type.FriendRequestStatus;

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
                               FriendRequest request,
                               Consumer<Observable<ApiResponse<Boolean>>> onAuthSuccess,
                               Consumer<Exception> onAuthFail);

    void requestConnect(FirebaseUser user,
                        FriendRequest request,
                        Consumer<Observable<ApiResponse<Boolean>>> onAuthSuccess,
                        Consumer<Exception> onAuthFail);

    Observable<ApiResponse<Boolean>> requestConnect(String uid);

    Observable<ApiResponse<Boolean>> cancelRequest(String uid);

    Observable<ApiResponse<List<String>>> getFriendListId(String token);

    Observable<Boolean> isFriend(String userId);
}
