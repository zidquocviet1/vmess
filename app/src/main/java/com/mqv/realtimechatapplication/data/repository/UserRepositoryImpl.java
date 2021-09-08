package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.service.UserService;
import com.mqv.realtimechatapplication.util.Const;

import java.util.Objects;
import java.util.function.Consumer;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Response;

public class UserRepositoryImpl implements UserRepository {
    private final UserService userService;

    @Inject
    public UserRepositoryImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Observable<Response<String>> addUser(String uid) {
        return userService.addUser(uid);
    }

    @Override
    public void fetchUserFromRemote(User remoteUser,
                                    @NonNull FirebaseUser user,
                                    @NonNull Consumer<Observable<ApiResponse<User>>> callback) {
        /*
         * Target remote user uid. If null it's mean clients want to load themself account information
         * */
        var uid = remoteUser != null ? remoteUser.getUid() : user.getUid();

        // Required Firebase User token to make sure clients are using the app to make a call
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = Objects.requireNonNull(task.getResult()).getToken();

                        callback.accept(userService.fetchUserFromRemote(
                                Const.PREFIX_TOKEN + token,
                                Const.DEFAULT_AUTHORIZER,
                                uid));
                    } else {
                        callback.accept(null);
                        var e = task.getException();
                        Objects.requireNonNull(e).printStackTrace();
                    }
                });
    }
}
