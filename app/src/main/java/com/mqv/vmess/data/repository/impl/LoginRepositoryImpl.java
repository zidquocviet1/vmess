package com.mqv.vmess.data.repository.impl;

import androidx.annotation.NonNull;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mqv.vmess.BuildConfig;
import com.mqv.vmess.data.dao.HistoryLoggedInUserDao;
import com.mqv.vmess.data.dao.UserDao;
import com.mqv.vmess.data.model.HistoryLoggedInUser;
import com.mqv.vmess.data.repository.LoginRepository;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.network.service.UserService;
import com.mqv.vmess.util.Const;
import com.mqv.vmess.util.Logging;

import java.net.HttpURLConnection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Response;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class LoginRepositoryImpl implements LoginRepository {
    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    private final UserService service;
    private final UserDao userDao;
    private final HistoryLoggedInUserDao historyUserDao;

    @Inject
    public LoginRepositoryImpl(UserService service, UserDao userDao, HistoryLoggedInUserDao historyUserDao) {
        this.service = service;
        this.userDao = userDao;
        this.historyUserDao = historyUserDao;
    }

    @Override
    public void loginWithUidAndToken(@NonNull FirebaseUser user,
                                     Consumer<Observable<ApiResponse<User>>> onAuthSuccess,
                                     Consumer<Exception> onAuthError) {
        // Required Firebase User token to make sure clients are using the app to make a call
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = Objects.requireNonNull(task.getResult()).getToken();

                        onAuthSuccess.accept(service.loginWithToken(
                                Const.PREFIX_TOKEN + token,
                                Const.DEFAULT_AUTHORIZER));
                    } else {
                        onAuthError.accept(task.getException());
                    }
                });
    }

    @Override
    public Completable saveLoggedInUser(User user, HistoryLoggedInUser historyUser) {
        return userDao.save(user).andThen(historyUserDao.save(historyUser));
    }

    @Override
    public void login(AuthCredential credential,
                      Consumer<Exception> onFirebaseLoginFail,
                      BiConsumer<Observable<ApiResponse<User>>, FirebaseUser> onAuthTokenSuccess,
                      Consumer<Exception> onAuthTokenFail) {
        FirebaseAuth.getInstance()
                .signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var result = task.getResult();
                        if (result != null) {
                            var user = result.getUser();
                            if (user != null) {
                                // Required Firebase User token to make sure clients are using the app to make a call
                                user.getIdToken(true)
                                        .addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                var token = Objects.requireNonNull(task2.getResult()).getToken();

                                                onAuthTokenSuccess.accept(service.loginWithToken(
                                                        Const.PREFIX_TOKEN + token,
                                                        Const.DEFAULT_AUTHORIZER), user);
                                            } else
                                                onAuthTokenFail.accept(task.getException());
                                        });
                            }
                        }
                    } else
                        onFirebaseLoginFail.accept(task.getException());
                });
    }

    @Override
    public void logout(FirebaseUser previousUser) {
        callFcmToken(fcmToken -> validateIdToken(previousUser,
                token -> service
                        .logout(Const.PREFIX_TOKEN + token, Const.DEFAULT_AUTHORIZER, fcmToken)
                        .subscribeOn(Schedulers.io())
                        .subscribe(new Observer<>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onNext(@NonNull ApiResponse<Boolean> response) {
                                if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                    Logging.show("Logout successfully");
                                }
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {

                            }

                            @Override
                            public void onComplete() {

                            }
                        }),
                Throwable::printStackTrace), Throwable::printStackTrace);
    }

    @Override
    public void logoutWithObserve(FirebaseUser previousUser,
                                  Consumer<Observable<ApiResponse<Boolean>>> onSuccess,
                                  Consumer<Exception> onError) {
        callFcmToken(fcmToken -> validateIdToken(previousUser,
                token -> onSuccess.accept(service
                        .logout(Const.PREFIX_TOKEN + token, Const.DEFAULT_AUTHORIZER, fcmToken)),
                onError), onError);
    }

    @Override
    public void sendFcmToken(FirebaseUser currentUser) {
        callFcmToken(fcmToken -> validateIdToken(currentUser,
                token -> service
                        .sendFcmTokenToServer(Const.PREFIX_TOKEN + token, Const.DEFAULT_AUTHORIZER, fcmToken)
                        .subscribeOn(Schedulers.io())
                        .subscribe(new Observer<>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onNext(@NonNull ApiResponse<Object> response) {
                                if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                    Logging.show("Send fcm token to server successfully");
                                } else {
                                    Logging.show("Send fcm token to server failure");
                                }
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                e.printStackTrace();
                            }

                            @Override
                            public void onComplete() {

                            }
                        }),
                Throwable::printStackTrace), Throwable::printStackTrace);
    }

    @Override
    public Observable<Response<ApiResponse<User>>> loginForDemoSection() {
        return service.loginForDemoSection(BuildConfig.DEMO_LOGIN_TOKEN);
    }

    private void validateIdToken(FirebaseUser user,
                                 Consumer<String> onAuthSuccess,
                                 Consumer<Exception> onAuthFail) {
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = Objects.requireNonNull(task.getResult()).getToken();

                        onAuthSuccess.accept(token);
                    } else {
                        onAuthFail.accept(task.getException());
                    }
                });
    }

    private void callFcmToken(Consumer<String> onRetrieve,
                              Consumer<Exception> onError) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        var token = task.getResult();
                        onRetrieve.accept(token);
                    } else {
                        onError.accept(task.getException());
                    }
                });
    }
}