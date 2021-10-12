package com.mqv.realtimechatapplication.data.repository;

import androidx.annotation.NonNull;

import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.model.Notification;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;

public interface NotificationRepository {
    Observable<List<Notification>> fetchNotificationNBR(int duration);

    Observable<List<Notification>> refreshNotificationList(int duration);

    Observable<ApiResponse<List<Notification>>> fetchNotification(String uid,
                                                                  int duration);

    Observable<ApiResponse<Notification>> markAsRead(Notification notification);

    Completable deleteAllLocal();

    Completable deleteLocal(Notification notification);

    Completable updateLocal(Notification notification);

    Observable<ApiResponse<Integer>> getUnreadNotification(int duration);

    Observable<ApiResponse<Notification>> removeNotification(@NonNull Notification notification);

    Flowable<List<Notification>> getUnreadNotificationCached();

    Completable saveCachedNotification(List<Notification> notifications);
}
