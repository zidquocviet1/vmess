package com.mqv.realtimechatapplication.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.dao.HistoryLoggedInUserDao;
import com.mqv.realtimechatapplication.data.dao.NotificationDao;
import com.mqv.realtimechatapplication.data.dao.UserDao;
import com.mqv.realtimechatapplication.data.model.HistoryLoggedInUser;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.exception.FirebaseUnauthorizedException;
import com.mqv.realtimechatapplication.network.model.Notification;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.service.NotificationService;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.util.UserTokenUtil;

import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltWorker
public class FetchNotificationWorker extends Worker {
    private static final String TAG = FetchNotificationWorker.class.getCanonicalName();

    private final NotificationService service;
    private final NotificationDao dao;
    private final UserDao userDao;
    private final HistoryLoggedInUserDao historyLoggedInUserDao;

    @AssistedInject
    public FetchNotificationWorker(@Assisted @NonNull Context context,
                                   @Assisted @NonNull WorkerParameters workerParams,
                                   NotificationService service,
                                   NotificationDao dao,
                                   UserDao userDao,
                                   HistoryLoggedInUserDao historyLoggedInUserDao) {
        super(context, workerParams);
        this.service = service;
        this.dao = dao;
        this.userDao = userDao;
        this.historyLoggedInUserDao = historyLoggedInUserDao;
    }

    @NonNull
    @Override
    public Result doWork() {
        Logging.show(TAG + ": Start fetching new notification list");

        shouldFetch();

        return Result.success();
    }

    private Observable<ApiResponse<List<Notification>>> getNotificationObservable() {
        var user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            return Observable.fromCallable(() -> UserTokenUtil.getToken(user))
                    .flatMap(optionalToken -> {
                        if (optionalToken.isPresent()) {
                            var token = optionalToken.get();
                            var bearerToken = Const.PREFIX_TOKEN + token;
                            var authorizer = Const.DEFAULT_AUTHORIZER;

                            return service.fetchNotification(bearerToken, authorizer, user.getUid(), 1);
                        } else {
                            return Observable.error(new FirebaseUnauthorizedException(R.string.error_authentication_fail));
                        }
                    });
        } else {
            return Observable.error(new FirebaseUnauthorizedException(R.string.error_user_id_not_found));
        }
    }

    private void saveNotificationList(List<Notification> notifications) {
        dao.save(notifications)
                .andThen(dao.deleteById(notifications
                        .stream()
                        .map(Notification::getId)
                        .collect(Collectors.toList())))
                .subscribeOn(Schedulers.io())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        Logging.show(TAG + ": Save notification list into database complete");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Logging.show(TAG + ": Save notification list into database error");
                    }
                });
    }

    private Single<HistoryLoggedInUser> getLoggedInUser() {
        return historyLoggedInUserDao.getLoggedInUser()
                .subscribeOn(Schedulers.io());
    }

    private void shouldFetch() {
        getLoggedInUser()
                .flatMap(user -> userDao.findByUid(user.getUid()))
                .subscribe(new DisposableSingleObserver<>() {
                    @Override
                    public void onSuccess(@NonNull User user) {
                        var fetchTimeOut = 30;
                        var now = LocalDateTime.now();
                        var shouldFetch = user.getAccessedDate().plusSeconds(fetchTimeOut).compareTo(now) <= 0;

                        if (shouldFetch) {
                            fetchNotification();
                        }else{
                            Logging.show(TAG + ": Recent login, not need to refresh new notification list");
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    private void fetchNotification() {
        getNotificationObservable()
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull ApiResponse<List<Notification>> response) {
                        if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                            saveNotificationList(response.getSuccess());
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Logging.show(TAG + ": Fetch notification list into from server error");
                    }

                    @Override
                    public void onComplete() {
                        Logging.show(TAG + ": Complete fetching notification list");
                    }
                });
    }
}
