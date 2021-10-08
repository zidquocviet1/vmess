package com.mqv.realtimechatapplication.data.repository.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.dao.NotificationDao;
import com.mqv.realtimechatapplication.data.repository.NotificationRepository;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.NetworkBoundResource;
import com.mqv.realtimechatapplication.network.exception.FirebaseUnauthorizedException;
import com.mqv.realtimechatapplication.network.model.Notification;
import com.mqv.realtimechatapplication.network.service.NotificationService;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.util.UserTokenUtil;

import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NotificationRepositoryImpl implements NotificationRepository {
    private final NotificationService service;
    private final NotificationDao dao;
    private final FirebaseUser user;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Inject
    public NotificationRepositoryImpl(NotificationService service,
                                      NotificationDao dao) {
        this.service = service;
        this.dao = dao;
        this.user = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public Observable<ApiResponse<List<Notification>>> fetchNotification(String uid, int duration) {
        return Observable.fromFuture(futureToken())
                .flatMap(optionalToken -> {
                    if (optionalToken.isPresent()) {
                        var bearerToken = Const.PREFIX_TOKEN + optionalToken.get();
                        var defaultAuthorizer = Const.DEFAULT_AUTHORIZER;

                        return service.fetchNotification(bearerToken, defaultAuthorizer, uid, duration);
                    } else {
                        return Observable.error(new FirebaseUnauthorizedException(R.string.error_authentication_fail));
                    }
                }).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<List<Notification>> fetchNotificationNBR(int duration) {
        return new NetworkBoundResource<List<Notification>, ApiResponse<List<Notification>>>(false) {
            @Override
            protected void saveCallResult(@NonNull ApiResponse<List<Notification>> response) {
                if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                    saveListNotification(response.getSuccess());
                }
            }

            @Override
            protected Boolean shouldFetch(@Nullable List<Notification> data) {
                if (user == null)
                    return false;

                if (data == null || data.isEmpty())
                    return true;

                var time = data.stream()
                        .map(p -> p.getAccessedDate().plusMinutes(10).compareTo(LocalDateTime.now()))
                        .filter(i -> i <= 0)
                        .collect(Collectors.toList());

                return !time.isEmpty();
            }

            @Override
            protected Flowable<List<Notification>> loadFromDb() {
                return dao.fetchAll();
            }

            @Override
            protected Observable<ApiResponse<List<Notification>>> createCall() {
                return fetchNotification(user.getUid(), duration);
            }

            @Override
            protected void callAndSaveResult() {

            }
        }.asObservable();
    }

    @Override
    public Observable<List<Notification>> refreshNotificationList(int duration) {
        return Observable.create(emitter -> fetchNotification(user.getUid(), duration)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        if (!d.isDisposed())
                            emitter.setDisposable(d);
                    }

                    @Override
                    public void onNext(@NonNull ApiResponse<List<Notification>> response) {
                        if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                            var data = response.getSuccess();

                            saveListNotification(data);

                            emitter.onNext(data);
                        } else if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                            emitter.onError(new FirebaseUnauthorizedException(R.string.error_authentication_fail));
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        emitter.onError(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                }));
    }

    @Override
    public Observable<ApiResponse<Notification>> markAsRead(Notification notification) {
        return Observable.fromFuture(futureToken())
                .subscribeOn(Schedulers.io())
                .flatMap(optionalToken -> {
                    if (optionalToken.isPresent()) {
                        var bearerToken = Const.PREFIX_TOKEN + optionalToken.get();
                        var defaultAuthorizer = Const.DEFAULT_AUTHORIZER;

                        return service.markAsRead(bearerToken, defaultAuthorizer, notification);
                    } else {
                        return Observable.error(new FirebaseUnauthorizedException(R.string.error_authentication_fail));
                    }
                });
    }

    private CompletableFuture<Optional<String>> futureToken() {
        return CompletableFuture.supplyAsync(() -> UserTokenUtil.getToken(user), executorService);
    }

    private void saveListNotification(List<Notification> notifications) {
        dao.save(notifications)
                .subscribeOn(Schedulers.io())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        Logging.show("Save notification list into database successfully");
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {

                    }
                });
    }
}
