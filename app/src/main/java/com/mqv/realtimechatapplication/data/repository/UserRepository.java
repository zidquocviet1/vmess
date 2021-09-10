package com.mqv.realtimechatapplication.data.repository;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;

import java.util.List;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import retrofit2.Response;

public interface UserRepository {
    Observable<Response<String>> addUser(String uid);

    /*
     * Using this method will help us easy to use from another ViewModel
     * */
    void fetchUserFromRemote(User remoteUser,
                             FirebaseUser user,
                             Consumer<Observable<ApiResponse<User>>> callback);

    Observable<List<User>> fetchUserUsingNBS(User remoteUser,
                                             FirebaseUser user);

    Completable insertUser(User user);
}
