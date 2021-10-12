package com.mqv.realtimechatapplication.work;

import static com.mqv.realtimechatapplication.network.model.type.NotificationType.ACCEPTED_FRIEND_REQUEST;
import static com.mqv.realtimechatapplication.network.model.type.NotificationType.NEW_FRIEND_REQUEST;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.dao.NotificationDao;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.exception.FirebaseUnauthorizedException;
import com.mqv.realtimechatapplication.network.model.Notification;
import com.mqv.realtimechatapplication.network.model.type.NotificationType;
import com.mqv.realtimechatapplication.network.service.NotificationService;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;
import com.mqv.realtimechatapplication.util.UserTokenUtil;

import java.net.HttpURLConnection;
import java.util.Objects;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltWorker
public class MarkNotificationReadWorker extends Worker {
    private static final String TAG = MarkNotificationReadWorker.class.getCanonicalName();
    private final NotificationService service;
    private final NotificationDao dao;

    @AssistedInject
    public MarkNotificationReadWorker(@Assisted @NonNull Context context,
                                      @Assisted @NonNull WorkerParameters workerParams,
                                      NotificationService service,
                                      NotificationDao dao) {
        super(context, workerParams);
        this.service = service;
        this.dao = dao;
    }

    @NonNull
    @Override
    public Result doWork() {
        Logging.show(TAG + ": Start mark notification as read");

        String uid = getInputData().getString(Const.KEY_UID);
        String agentId = getInputData().getString(Const.KEY_AGENT_ID);
        String agentImageUrl = getInputData().getString(Const.KEY_IMAGE_URL);
        String typeString = getInputData().getString("type");
        NotificationType type = Objects.requireNonNull(typeString).equals(Const.DEFAULT_NEW_FRIEND_REQUEST) ?
                NEW_FRIEND_REQUEST : ACCEPTED_FRIEND_REQUEST;

        var notification = new Notification(type, uid, agentId);

        findByUid(notification)
                .subscribeOn(Schedulers.io())
                .doOnError(Throwable::printStackTrace)
                .flatMap((Function<ApiResponse<Notification>, ObservableSource<ApiResponse<Notification>>>) response -> {
                    if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                        var data = response.getSuccess();

                        return markNotificationReadObservable(data);
                    } else {
                        return Observable.error(new FirebaseUnauthorizedException(R.string.error_authentication_fail));
                    }
                })
                .subscribe(new DisposableObserver<>() {
                    @Override
                    public void onNext(@NonNull ApiResponse<Notification> response) {
                        if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                            var updatedNotification = response.getSuccess();

                            updatedNotification.setAgentImageUrl(agentImageUrl);

                            updateLocalNotification(updatedNotification);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        return Result.success();
    }

    private Observable<ApiResponse<Notification>> findByUid(Notification notification) {
        var user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            return Observable.fromCallable(() -> UserTokenUtil.getToken(user))
                    .flatMap(optionalToken -> {
                        if (optionalToken.isPresent()) {
                            var token = optionalToken.get();
                            var bearerToken = Const.PREFIX_TOKEN + token;
                            var authorizer = Const.DEFAULT_AUTHORIZER;

                            return service.findByUidAndAgentId(bearerToken, authorizer, notification);
                        } else {
                            return Observable.error(new FirebaseUnauthorizedException(R.string.error_authentication_fail));
                        }
                    });
        } else {
            return Observable.error(new FirebaseUnauthorizedException(R.string.error_user_id_not_found));
        }
    }

    private void updateLocalNotification(Notification updatedNotification) {
        dao.update(updatedNotification)
                .subscribeOn(Schedulers.io())
                .subscribe(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        Logging.show(TAG + ": Update notification from Worker successfully");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    private Observable<ApiResponse<Notification>> markNotificationReadObservable(Notification item) {
        var user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            return Observable.fromCallable(() -> UserTokenUtil.getToken(user))
                    .flatMap(optionalToken -> {
                        if (optionalToken.isPresent()) {
                            var token = optionalToken.get();
                            var bearerToken = Const.PREFIX_TOKEN + token;
                            var authorizer = Const.DEFAULT_AUTHORIZER;

                            return service.markAsRead(bearerToken, authorizer, item);
                        } else {
                            return Observable.error(new FirebaseUnauthorizedException(R.string.error_authentication_fail));
                        }
                    });
        } else {
            return Observable.error(new FirebaseUnauthorizedException(R.string.error_user_id_not_found));
        }
    }
}
