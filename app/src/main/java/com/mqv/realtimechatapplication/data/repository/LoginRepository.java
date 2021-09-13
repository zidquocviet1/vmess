package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.data.model.LoggedInUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.util.Resource;

import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Observable;

public interface LoginRepository {
    void loginWithUidAndToken(@NonNull FirebaseUser user,
                              Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                              Consumer<Exception> onAuthError);
}
