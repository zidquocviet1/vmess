package com.mqv.vmess.data.repository.impl;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.data.dao.FriendNotificationDao;
import com.mqv.vmess.data.model.FriendNotification;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.network.ApiResponse;
import com.mqv.vmess.network.service.NotificationService;
import com.mqv.vmess.util.UserTokenUtil;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NotificationRepositoryImpl implements NotificationRepository {
    private final NotificationService   service;
    private final FriendNotificationDao dao;
    private FirebaseUser user;

    @Inject
    public NotificationRepositoryImpl(NotificationService service,
                                      FriendNotificationDao friendNotificationDao) {
        this.service = service;
        this.dao     = friendNotificationDao;
        this.user    = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth ->
                user = firebaseAuth.getCurrentUser());
    }

    @Override
    public Flowable<List<FriendNotification>> observeFriendNotification() {
        return dao.fetchAll();
    }

    @Override
    public Flowable<Integer> observeUnreadFriendNotification() {
        return dao.fetchUnreadNotification();
    }

    @Override
    public Single<FriendNotification> fetchById(Long id) {
        return dao.fetchById(id);
    }

    @Override
    public Single<FriendNotification> fetchRequestNotificationBySenderId(String senderId) {
        return dao.fetchRequestNotificationByUserId(senderId);
    }

    @Override
    public Single<FriendNotification> fetchAcceptedNotificationByUserId(String userId) {
        return dao.fetchAcceptedNotificationByUserId(userId);
    }

    @Override
    public Single<List<FriendNotification>> fetchAllNotificationRelatedToUser(String userId) {
        return dao.fetchAllNotificationRelatedToUser(userId);
    }

    @Override
    public Observable<ApiResponse<List<FriendNotification>>> fetchNotification(int duration) {
        return UserTokenUtil.getTokenSingle(user)
                            .flatMapObservable(token -> service.fetchNotification(token, duration));
    }

    @Override
    public Observable<ApiResponse<FriendNotification>> markAsRead(FriendNotification notification) {
        notification.setHasRead(true);

        return dao.update(notification)
                  .andThen(UserTokenUtil.getTokenSingle(user).toObservable())
                  .flatMap(token -> service.markAsRead(token, notification));
    }

    @Override
    public Completable deleteAllLocal() {
        return dao.deleteAll();
    }

    @Override
    public Observable<ApiResponse<FriendNotification>> removeNotification(@NonNull FriendNotification notification) {
        return dao.delete(notification)
                  .andThen(UserTokenUtil.getTokenSingle(user).toObservable())
                  .flatMap(token -> service.removeNotification(token, notification));
    }

    @Override
    public Completable saveCachedNotification(List<FriendNotification> notifications) {
        return saveListNotification(notifications);
    }

    @Override
    public Completable delete(List<FriendNotification> notifications) {
        return dao.delete(notifications);
    }

    private Completable saveListNotification(List<FriendNotification> notifications) {
        return dao.insert(notifications)
                  .andThen(dao.deleteById(notifications.stream()
                                                       .map(FriendNotification::getId)
                                                       .collect(Collectors.toList())))
                  .subscribeOn(Schedulers.io());
    }
}
