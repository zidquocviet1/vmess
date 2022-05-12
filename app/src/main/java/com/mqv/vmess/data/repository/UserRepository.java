package com.mqv.vmess.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.ui.data.PhoneContact;

import java.util.List;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;

public interface UserRepository {
    Completable addUserToDb(User user);

    /*
     * Using this method will help us easy to use from another ViewModel
     * */
    Observable<ApiResponse<User>> fetchUserFromRemote(@Nullable String uid);

    Observable<ApiResponse<String>> registerEmailAndPassword(@NonNull String email,
                                                             @NonNull String password,
                                                             @NonNull String displayName);

    void editUser(@NonNull User updateUser,
                  @NonNull FirebaseUser user,
                  Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                  Consumer<Exception> onAuthError);

    void editUserDisplayName(@NonNull User updateUser,
                             @NonNull FirebaseUser user,
                             Consumer<Observable<ApiResponse<String>>> onAuthSuccess,
                             Consumer<Exception> onAuthError);

    void editUserConnectName(@NonNull User updateUser,
                             @NonNull FirebaseUser user,
                             Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                             Consumer<Exception> onAuthError);

    void checkUserConnectName(@NonNull String username,
                              @NonNull FirebaseUser user,
                              Consumer<Observable<ApiResponse<Boolean>>> onAuthSuccess,
                              Consumer<Exception> onAuthError);

    void getConnectUserByQrCode(@NonNull String code,
                                @NonNull FirebaseUser user,
                                Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                                Consumer<Exception> onAuthError);

    void getConnectUserByUsername(@NonNull String username,
                                  @NonNull FirebaseUser user,
                                  Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                                  Consumer<Exception> onAuthError);

    void getConnectUserByUid(FirebaseUser user,
                             @NonNull String uid,
                             Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                             Consumer<Exception> onAuthError);

    Observable<List<User>> fetchUserUsingNBS(User remoteUser,
                                             FirebaseUser user);

    Observable<ApiResponse<PhoneContact>> fetchPhoneContactInfo(String number);
}
