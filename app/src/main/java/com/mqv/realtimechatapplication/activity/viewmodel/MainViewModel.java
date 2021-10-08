package com.mqv.realtimechatapplication.activity.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.repository.NotificationRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.ui.data.People;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainViewModel extends AbstractMainViewModel {

    @Inject
    public MainViewModel(UserRepository userRepository,
                         FriendRequestRepository friendRequestRepository,
                         PeopleRepository peopleRepository,
                         NotificationRepository notificationRepository) {
        super(userRepository, friendRequestRepository, peopleRepository, notificationRepository);

        loadRemoteUserUsingNBR();
        loadAllPeople();
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

    public LiveData<Integer> getBadgeNotificationSafe() {
        // TODO: not complete here
        return Transformations.map(getNotificationList(), result -> {
            if (result == null)
                return 0;
            if (result.getStatus() == NetworkStatus.SUCCESS){
                var data = result.getSuccess();

                if (data == null || data.isEmpty())
                    return 0;

                return (int) data.stream()
                        .filter(n -> !n.getHasRead()).count();
            }

            return 0;
        });
    }
}
