package com.mqv.realtimechatapplication.data.datasource;

import com.mqv.realtimechatapplication.data.model.LoggedInUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.CustomUser;
import com.mqv.realtimechatapplication.network.service.UserService;
import com.mqv.realtimechatapplication.util.Resource;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {
    private final UserService service;

    @Inject
    public LoginDataSource(UserService service){
        this.service = service;
    }

    public Resource<LoggedInUser> login(String username, String password) {
        try {
            // TODO: handle loggedInUser authentication
            LoggedInUser fakeUser =
                    new LoggedInUser(
                            java.util.UUID.randomUUID().toString(),
                            "Jane Doe");
            return Resource.Success(fakeUser);
        } catch (Exception e) {
            return Resource.Error("Error logging in");
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }

    public Observable<ApiResponse<CustomUser>> fetchCustomUserInfo(String token){
        return service.fetchCustomUserInfo(token, "firebase");
    }
}