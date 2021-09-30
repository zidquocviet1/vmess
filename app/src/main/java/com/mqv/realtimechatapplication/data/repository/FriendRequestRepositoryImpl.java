package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.FriendRequest;
import com.mqv.realtimechatapplication.network.model.type.FriendRequestStatus;
import com.mqv.realtimechatapplication.network.service.FriendRequestService;
import com.mqv.realtimechatapplication.util.Const;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

public class FriendRequestRepositoryImpl implements FriendRequestRepository {
    private final FriendRequestService service;
    private final FirebaseUser user;

    @Inject
    public FriendRequestRepositoryImpl(FriendRequestService service) {
        this.service = service;
        this.user = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public void getAllPendingRequest(Consumer<Observable<ApiResponse<List<FriendRequest>>>> onAuthSuccess,
                                     Consumer<Exception> onAuthFail) {
        validateIdToken(token -> onAuthSuccess.accept(
                service.getAllPendingRequest(Const.PREFIX_TOKEN + token, Const.DEFAULT_AUTHORIZER)),
                onAuthFail);
    }

    @Override
    public void findFriendRequestStatusByReceiverId(@NonNull String receiverId,
                                                    Consumer<Observable<ApiResponse<FriendRequestStatus>>> onAuthSuccess,
                                                    Consumer<Exception> onAuthFail) {
        validateIdToken(token -> onAuthSuccess.accept(
                service.findFriendRequestStatusByReceiverId(Const.PREFIX_TOKEN + token,
                        Const.DEFAULT_AUTHORIZER,
                        receiverId)),
                onAuthFail);
    }

    @Override
    public void responseFriendRequest(FriendRequest request,
                                      Consumer<Observable<ApiResponse<Boolean>>> onAuthSuccess,
                                      Consumer<Exception> onAuthFail) {
        validateIdToken(token -> onAuthSuccess.accept(service.responseFriendRequest(Const.PREFIX_TOKEN + token,
                Const.DEFAULT_AUTHORIZER,
                request)),
                onAuthFail);
    }

    @Override
    public void requestConnect(FriendRequest request,
                               Consumer<Observable<ApiResponse<Boolean>>> onAuthSuccess,
                               Consumer<Exception> onAuthFail) {
        validateIdToken(token -> onAuthSuccess.accept(service.requestConnect(Const.PREFIX_TOKEN + token,
                Const.DEFAULT_AUTHORIZER,
                request)),
                onAuthFail);
    }

    private void validateIdToken(Consumer<String> onAuthSuccess,
                                 Consumer<Exception> onAuthFail) {
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var result = task.getResult();
                        var token = Objects.requireNonNull(result).getToken();

                        onAuthSuccess.accept(token);
                    } else {
                        onAuthFail.accept(task.getException());
                    }
                });
    }
}
