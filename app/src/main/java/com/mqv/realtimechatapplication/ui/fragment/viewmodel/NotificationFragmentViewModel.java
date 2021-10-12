package com.mqv.realtimechatapplication.ui.fragment.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.firebase.FirebaseNetworkException;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.AbstractMainViewModel;
import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.repository.NotificationRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.ApiResponse;
import com.mqv.realtimechatapplication.network.exception.FirebaseUnauthorizedException;
import com.mqv.realtimechatapplication.network.model.Notification;
import com.mqv.realtimechatapplication.util.Logging;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class NotificationFragmentViewModel extends AbstractMainViewModel {
    private final NotificationRepository notificationRepository;
    private       Disposable             loadNotificationDisposable;

    private final MutableLiveData<Result<List<Notification>>> notificationListResult        = new MutableLiveData<>();
    private final MutableLiveData<Result<List<Notification>>> refreshResult                 = new MutableLiveData<>();
    private final List<Notification>                          mDeleteItemList               = new ArrayList<>();

    @Inject
    public NotificationFragmentViewModel(UserRepository userRepository,
                                         FriendRequestRepository friendRequestRepository,
                                         PeopleRepository peopleRepository,
                                         NotificationRepository notificationRepository) {
        super(userRepository, friendRequestRepository, peopleRepository, notificationRepository);

        this.notificationRepository = notificationRepository;
    }

    @Override
    public void onRefresh() {
        refreshResult.setValue(Result.Loading());

        cd.add(notificationRepository
                .refreshNotificationList(NOTIFICATION_DURATION_LIMIT)
                .doOnDispose(() -> refreshResult.setValue(Result.Terminate()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> refreshResult.setValue(Result.Success(data)), t -> {
                    if (t instanceof FirebaseUnauthorizedException) {
                        refreshResult.setValue(Result.Fail(((FirebaseUnauthorizedException) t).getError()));
                    } else if (t instanceof FirebaseNetworkException) {
                        refreshResult.setValue(Result.Fail(R.string.error_network_connection));
                    } else if (t instanceof SocketTimeoutException) {
                        refreshResult.setValue(Result.Fail(R.string.error_connect_server_fail));
                    } else {
                        refreshResult.setValue(Result.Fail(R.string.error_unknown));
                    }
                }));
    }

    public LiveData<Result<List<Notification>>> getRefreshResultSafe() {
        return refreshResult;
    }

    public LiveData<Result<List<Notification>>> getNotificationListResult() {
        return Transformations.distinctUntilChanged(notificationListResult);
    }

    public List<Notification> getDeleteItemList() {
        return mDeleteItemList;
    }

    public void markAsRead(Notification item) {
        notificationRepository.markAsRead(item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<>() {
                    @Override
                    public void onNext(@NonNull ApiResponse<Notification> response) {
                        if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                            var updated = response.getSuccess();

                            updated.setAgentImageUrl(item.getAgentImageUrl());

                            updateLocalNotification(response.getSuccess());
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
    }

    public void removeNotification(Notification notification) {
        notificationRepository.removeNotification(notification)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<>() {
                    @Override
                    public void onNext(@NonNull ApiResponse<Notification> response) {
                        if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                            notificationRepository.deleteLocal(response.getSuccess())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new DisposableCompletableObserver() {
                                        @Override
                                        public void onComplete() {
                                            Logging.show("Remove notification cached item successfully.");
                                        }

                                        @Override
                                        public void onError(@NonNull Throwable e) {
                                            e.printStackTrace();
                                        }
                                    });
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
    }

    public void addRemoveItemList(Notification notification) {
        this.mDeleteItemList.add(notification);
    }

    private void updateLocalNotification(Notification updatedItem) {
        notificationRepository.updateLocal(updatedItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
    }

    public void loadNotificationUsingNBR() {
        loadNotificationDisposable = notificationRepository
                .fetchNotificationNBR(NOTIFICATION_DURATION_LIMIT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> notificationListResult.setValue(Result.Success(data)),
                        e -> {
                            if (e instanceof FirebaseUnauthorizedException) {
                                notificationListResult.setValue(Result.Fail(((FirebaseUnauthorizedException) e).getError()));
                            } else {
                                notificationListResult.setValue(Result.Fail(R.string.error_unknown));
                            }
                        });
        cd.add(loadNotificationDisposable);
    }

    public void forceDispose() {
        if (loadNotificationDisposable != null && !loadNotificationDisposable.isDisposed())
            loadNotificationDisposable.dispose();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDeleteItemList.clear();
    }
}
