package com.mqv.realtimechatapplication.data.repository;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;

import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Response;

public interface UserRepository {
    Observable<Response<String>> addUser(String uid);

    /*
     * Using this method will help us easy to use from another ViewModel
     * */
    void fetchUserFromRemote(User remoteUser,
                             FirebaseUser user,
                             Consumer<Observable<ApiResponse<User>>> callback);
}
