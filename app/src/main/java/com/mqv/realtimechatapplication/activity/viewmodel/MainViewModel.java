package com.mqv.realtimechatapplication.activity.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.ui.data.People;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainViewModel extends AbstractMainViewModel {

    @Inject
    public MainViewModel(UserRepository userRepository,
                         FriendRequestRepository friendRequestRepository,
                         PeopleRepository peopleRepository) {
        super(userRepository, friendRequestRepository, peopleRepository);
    }

    @Override
    public void onRefresh() {

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
}
