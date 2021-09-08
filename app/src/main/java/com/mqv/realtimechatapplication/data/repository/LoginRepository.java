package com.mqv.realtimechatapplication.data.repository;

import com.mqv.realtimechatapplication.data.model.LoggedInUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.util.Resource;

import io.reactivex.rxjava3.core.Observable;

public interface LoginRepository {
    boolean isLoggedIn();

    void logout();

    Resource<LoggedInUser> login(String username, String password);

    Observable<ApiResponse<User>> fetchCustomUserInfo(String token);
}
