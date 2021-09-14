package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.service.UserService;
import com.mqv.realtimechatapplication.util.Const;

import java.util.Objects;
import java.util.function.Consumer;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class LoginRepositoryImpl implements LoginRepository {
    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    private final UserService service;
    private final UserDao userDao;

    @Inject
    public LoginRepositoryImpl(UserService service, UserDao userDao) {
        this.service = service;
        this.userDao = userDao;
    }

    @Override
    public void loginWithUidAndToken(@NonNull FirebaseUser user,
                                     Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                                     Consumer<Exception> onAuthError) {
        // Required Firebase User token to make sure clients are using the app to make a call
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = Objects.requireNonNull(task.getResult()).getToken();

                        onAuthSuccess.accept(service.loginWithToken(
                                Const.PREFIX_TOKEN + token,
                                Const.DEFAULT_AUTHORIZER));
                    } else {
                        onAuthError.accept(task.getException());
                    }
                });
    }

    @Override
    public Completable saveLoggedInUser(User user) {
        return userDao.save(user);
    }
}