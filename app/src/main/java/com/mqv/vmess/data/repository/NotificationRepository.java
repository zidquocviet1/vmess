package com.mqv.vmess.data.repository;

import androidx.annotation.NonNull;

import com.mqv.vmess.data.model.FriendNotification;
import com.mqv.vmess.network.ApiResponse;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface NotificationRepository {
    Flowable<List<FriendNotification>> observeFriendNotification();

    Flowable<Integer> observeUnreadFriendNotification();

    Single<FriendNotification> fetchById(Long id);

    Single<FriendNotification> fetchRequestNotificationBySenderId(String senderId);

    Single<FriendNotification> fetchAcceptedNotificationByUserId(String userId);

    Single<List<FriendNotification>> fetchAllNotificationRelatedToUser(String userId);

    Observable<ApiResponse<List<FriendNotification>>> fetchNotification(int duration);

    Observable<ApiResponse<FriendNotification>> markAsRead(FriendNotification notification);

    Observable<ApiResponse<FriendNotification>> removeNotification(@NonNull FriendNotification notification);

    Completable deleteAllLocal();

    Completable saveCachedNotification(List<FriendNotification> notifications);

    Completable delete(List<FriendNotification> notifications);
}
