package com.mqv.vmess.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.data.model.HistoryLoggedInUser;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.model.User;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import retrofit2.Response;

public interface LoginRepository {
    void loginWithUidAndToken(@NonNull FirebaseUser user,
                              Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                              Consumer<Exception> onAuthError);

    Completable saveLoggedInUser(User user, HistoryLoggedInUser historyUser);

    void login(AuthCredential credential,
               Consumer<Exception> onFirebaseLoginFail,
               BiConsumer<Observable<ApiResponse<User>>, FirebaseUser> onAuthTokenSuccess,
               Consumer<Exception> onAuthTokenFail);

    void logout(FirebaseUser previousUser);

    void logoutWithObserve(FirebaseUser previousUser,
                           Consumer<Observable<ApiResponse<Boolean>>> onSuccess,
                           Consumer<Exception> onError);

    void sendFcmToken(FirebaseUser currentUser);

    Observable<Response<ApiResponse<User>>> loginForDemoSection();
}
