package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.FriendRequest;
import com.mqv.realtimechatapplication.network.service.FriendRequestService;
import com.mqv.realtimechatapplication.util.Const;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

public class FriendRequestRepositoryImpl implements FriendRequestRepository{
    private final FriendRequestService service;

    @Inject
    public FriendRequestRepositoryImpl(FriendRequestService service) {
        this.service = service;
    }

    @Override
    public void getAllPendingRequest(@NonNull FirebaseUser firebaseUser,
                                     Consumer<Observable<ApiResponse<List<FriendRequest>>>> onAuthSuccess,
                                     Consumer<Exception> onAuthFail) {
        firebaseUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        var result = task.getResult();
                        var token = Objects.requireNonNull(result).getToken();

                        onAuthSuccess.accept(service.getAllPendingRequest(Const.PREFIX_TOKEN + token, Const.DEFAULT_AUTHORIZER));
                    }else{
                        onAuthFail.accept(task.getException());
                    }
                });
    }
}
