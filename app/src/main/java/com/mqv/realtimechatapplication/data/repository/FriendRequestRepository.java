package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.FriendRequest;

import java.util.List;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Observable;

public interface FriendRequestRepository {
    void getAllPendingRequest(@NonNull FirebaseUser firebaseUser,
                              Consumer<Observable<ApiResponse<List<FriendRequest>>>> onAuthSuccess,
                              Consumer<Exception> onAuthFail);
}
