package com.mqv.realtimechatapplication.ui.fragment.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.FirebaseNetworkException;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.AbstractMainViewModel;
import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.repository.NotificationRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.exception.FirebaseUnauthorizedException;
import com.mqv.realtimechatapplication.network.model.Notification;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class NotificationFragmentViewModel extends AbstractMainViewModel {
    private final NotificationRepository notificationRepository;

    private final MutableLiveData<Result<List<Notification>>> refreshResult = new MutableLiveData<>();
    private final MutableLiveData<Notification> updateNotification = new MutableLiveData<>();

    @Inject
    public NotificationFragmentViewModel(UserRepository userRepository,
                                         FriendRequestRepository friendRequestRepository,
                                         PeopleRepository peopleRepository,
                                         NotificationRepository notificationRepository) {
        super(userRepository, friendRequestRepository, peopleRepository, notificationRepository);

        this.notificationRepository = notificationRepository;

        loadNotificationUsingNBR();
    }

    @Override
    public void onRefresh() {
        refreshResult.setValue(Result.Loading());

        cd.add(notificationRepository
                .refreshNotificationList(NOTIFICATION_DURATION_LIMIT)
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

    public LiveData<Result<List<Notification>>> getNotificationListSafe() {
        return getNotificationList();
    }

    public LiveData<Result<List<Notification>>> getRefreshResultSafe() {
        return refreshResult;
    }

    public LiveData<Notification> getUpdateNotificationSafe() {
        return updateNotification;
    }

    public void markAsRead(Notification item) {
        cd.add(notificationRepository.markAsRead(item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                                updateNotification.setValue(response.getSuccess());
                            }
                        },
                        Throwable::printStackTrace));
    }

    public void resetUpdateResult() {
        updateNotification.setValue(null);
    }
}
