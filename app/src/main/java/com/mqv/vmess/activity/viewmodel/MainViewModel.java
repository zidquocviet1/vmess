package com.mqv.vmess.activity.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mqv.vmess.data.repository.FriendRequestRepository;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.repository.UserRepository;
import com.mqv.vmess.data.result.Result;
import com.mqv.vmess.network.model.User;
import com.mqv.vmess.ui.data.People;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class MainViewModel extends AbstractMainViewModel {
    private final NotificationRepository notificationRepository;

    private final MutableLiveData<Integer> notificationBadgeResult = new MutableLiveData<>();

    @Inject
    public MainViewModel(UserRepository userRepository,
                         FriendRequestRepository friendRequestRepository,
                         PeopleRepository peopleRepository,
                         NotificationRepository notificationRepository) {
        super(userRepository, friendRequestRepository, peopleRepository, notificationRepository);

        this.notificationRepository = notificationRepository;

        loadRemoteUserUsingNBR();
        loadAllPeople();
        loadNotificationBadge();
    }

    @Override
    public void onRefresh() {
        // default implementation
    }

    public LiveData<Uri> getUserPhotoUrl() {
        return Transformations.map(getFirebaseUser(), user -> {
            if (user == null)
                return null;
            return user.getPhotoUrl();
        });
    }

    public LiveData<Result<User>> getRemoteUserResultSafe() {
        return getRemoteUserResult();
    }

    public LiveData<List<People>> getListPeopleSafe() {
        return getListPeople();
    }

    public LiveData<Integer> getNotificationBadgeResult() {
        return notificationBadgeResult;
    }

    private void loadNotificationBadge() {
        cd.add(notificationRepository.getUnreadNotificationCached()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(notifications -> {
                    if (notifications == null || notifications.isEmpty()) {
                        notificationBadgeResult.setValue(0);
                    } else {
                        var count = (int) notifications.stream()
                                .filter(n -> !n.getHasRead())
                                .count();
                        notificationBadgeResult.setValue(count);
                    }
                }, t -> notificationBadgeResult.setValue(0)));
    }
}
