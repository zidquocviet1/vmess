package com.mqv.realtimechatapplication.data.repository.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.NetworkBoundResource;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.service.UserService;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class UserRepositoryImpl implements UserRepository {
    private final UserService userService;
    private final UserDao userDao;
    private final CompositeDisposable cd = new CompositeDisposable();

    @Inject
    public UserRepositoryImpl(UserService userService, UserDao userDao) {
        this.userService = userService;
        this.userDao = userDao;
    }

    @Override
    public void fetchUserFromRemote(User remoteUser,
                                    @NonNull FirebaseUser user,
                                    @NonNull Consumer<Observable<ApiResponse<User>>> callback) {
        /*
         * Target remote user uid. If null it's mean clients want to load themself account information
         * */
        var uid = remoteUser != null ? remoteUser.getUid() : user.getUid();

        // Required Firebase User token to make sure clients are using the app to make a call
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = Objects.requireNonNull(task.getResult()).getToken();

                        callback.accept(userService.fetchUserFromRemote(
                                Const.PREFIX_TOKEN + token,
                                Const.DEFAULT_AUTHORIZER,
                                uid));
                    } else {
                        callback.accept(null);
                        var e = task.getException();
                        Objects.requireNonNull(e).printStackTrace();
                    }
                });
    }

    @Override
    public void editUser(@NonNull User updateUser,
                         @NonNull FirebaseUser user,
                         Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                         Consumer<Exception> onAuthError) {
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = Objects.requireNonNull(task.getResult()).getToken();

                        onAuthSuccess.accept(userService.editRemoteUser(
                                Const.PREFIX_TOKEN + token,
                                Const.DEFAULT_AUTHORIZER,
                                updateUser));
                    } else {
                        onAuthError.accept(task.getException());
                    }
                });
    }

    @Override
    public void editUserDisplayName(@NonNull User updateUser,
                                    @NonNull FirebaseUser user,
                                    Consumer<Observable<ApiResponse<String>>> onAuthSuccess,
                                    Consumer<Exception> onAuthError) {
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = Objects.requireNonNull(task.getResult()).getToken();

                        onAuthSuccess.accept(userService.editUserDisplayName(
                                Const.PREFIX_TOKEN + token,
                                Const.DEFAULT_AUTHORIZER,
                                updateUser.getDisplayName()));
                    } else {
                        onAuthError.accept(task.getException());
                    }
                });
    }

    @Override
    public void editUserConnectName(@NonNull User updateUser,
                                    @NonNull FirebaseUser user,
                                    Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                                    Consumer<Exception> onAuthError) {
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = Objects.requireNonNull(task.getResult()).getToken();

                        onAuthSuccess.accept(userService.editUserConnectName(
                                Const.PREFIX_TOKEN + token,
                                Const.DEFAULT_AUTHORIZER,
                                updateUser));
                    } else {
                        onAuthError.accept(task.getException());
                    }
                });
    }

    @Override
    public void checkUserConnectName(@NonNull String username,
                                     @NonNull FirebaseUser user,
                                     Consumer<Observable<ApiResponse<Boolean>>> onAuthSuccess,
                                     Consumer<Exception> onAuthError) {
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = Objects.requireNonNull(task.getResult()).getToken();

                        onAuthSuccess.accept(userService.checkUserConnectName(
                                Const.PREFIX_TOKEN + token,
                                Const.DEFAULT_AUTHORIZER,
                                username));
                    } else {
                        onAuthError.accept(task.getException());
                    }
                });
    }

    @Override
    public void getConnectUserByQrCode(@NonNull String code,
                                       @NonNull FirebaseUser user,
                                       Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                                       Consumer<Exception> onAuthError) {
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = Objects.requireNonNull(task.getResult()).getToken();

                        onAuthSuccess.accept(userService.getConnectUserByQrCode(
                                Const.PREFIX_TOKEN + token, Const.DEFAULT_AUTHORIZER, code));
                    } else {
                        onAuthError.accept(task.getException());
                    }
                });
    }

    @Override
    public void getConnectUserByUsername(@NonNull String username,
                                         @NonNull FirebaseUser user,
                                         Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                                         Consumer<Exception> onAuthError) {
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = Objects.requireNonNull(task.getResult()).getToken();

                        onAuthSuccess.accept(userService.getConnectUserByUsername(
                                Const.PREFIX_TOKEN + token, Const.DEFAULT_AUTHORIZER, username));
                    } else {
                        onAuthError.accept(task.getException());
                    }
                });
    }

    @Override
    public void getConnectUserByUid(FirebaseUser user,
                                    @NonNull String uid,
                                    Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                                    Consumer<Exception> onAuthError) {
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = Objects.requireNonNull(task.getResult()).getToken();

                        onAuthSuccess.accept(userService.getConnectUserByUid(
                                Const.PREFIX_TOKEN + token, Const.DEFAULT_AUTHORIZER, uid));
                    } else {
                        onAuthError.accept(task.getException());
                    }
                });
    }

    @Override
    public Observable<List<User>> fetchUserUsingNBS(User remoteUser,
                                                    @NonNull FirebaseUser user) {
        var uid = remoteUser != null ? remoteUser.getUid() : user.getUid();

        return new NetworkBoundResource<List<User>, ApiResponse<User>>(true) {
            @Override
            protected void saveCallResult(@NonNull ApiResponse<User> item) {
                // Don't use this method when isCallInListener = true
            }

            @Override
            protected Boolean shouldFetch(@Nullable List<User> data) {
                if (data == null || data.isEmpty())
                    return true;

                var user = data.stream()
                        .filter(u -> u.getUid().equals(uid))
                        .findFirst()
                        .orElse(null);

                return (user == null) || (user.getAccessedDate().plusMinutes(10L).compareTo(LocalDateTime.now()) <= 0);
            }

            @Override
            protected Flowable<List<User>> loadFromDb() {
                return userDao.findByUid();
            }

            @Override
            protected Observable<ApiResponse<User>> createCall() {
                // Don't use this method when isCallInListener = true
                return null;
            }

            @Override
            protected void callAndSaveResult() {
                var uid = remoteUser != null ? remoteUser.getUid() : user.getUid();

                user.getIdToken(true)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                var token = Objects.requireNonNull(task.getResult()).getToken();

                                handleCallRemoteUser(Const.PREFIX_TOKEN + token, uid);
                            }
                        });
            }

            @Override
            protected void onFetchFailed(Throwable throwable) {
                throwable.printStackTrace();
            }
        }.asObservable();
    }

    private void handleCallRemoteUser(String token, String uid) {
        cd.add(userService.fetchUserFromRemote(token, Const.DEFAULT_AUTHORIZER, uid)
                .subscribeOn(Schedulers.io())
                .subscribe(response -> {
                    var code = response.getStatusCode();
                    if (code == HttpURLConnection.HTTP_OK) {
                        saveRemoteUserToDb(response.getSuccess());
                    }
                }, Throwable::printStackTrace));
    }

    private void saveRemoteUserToDb(User user) {
        userDao.save(user)
                .subscribeOn(Schedulers.io())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        Logging.show("Save remote user into database successfully with id = " + user.getUid());
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        Logging.show("Save remote user into database failure");
                    }
                });
    }

    @Override
    public Completable addUserToDb(User user) {
        return userDao.save(user);
    }
}
