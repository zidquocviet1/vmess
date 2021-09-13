package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;

import java.util.List;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.Response;

public interface UserRepository {
    Completable addUserToDb(User user);

    /*
     * Using this method will help us easy to use from another ViewModel
     * */
    void fetchUserFromRemote(User remoteUser,
                             FirebaseUser user,
                             Consumer<Observable<ApiResponse<User>>> callback);

    void editUser(@NonNull User updateUser,
                  @NonNull FirebaseUser user,
                  Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                  Consumer<Exception> onAuthError);

    Observable<List<User>> fetchUserUsingNBS(User remoteUser,
                                             FirebaseUser user);
}
