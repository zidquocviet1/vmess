package com.mqv.realtimechatapplication.data.repository;

import com.mqv.realtimechatapplication.network.service.UserService;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Response;

public class UserRepositoryImpl implements UserRepository{
    private final UserService userService;

    @Inject
    public UserRepositoryImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Observable<Response<String>> addUser(String uid) {
        return userService.addUser(uid);
    }
}
