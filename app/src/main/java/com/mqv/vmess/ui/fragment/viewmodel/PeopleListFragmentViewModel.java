package com.mqv.vmess.ui.fragment.viewmodel;

import androidx.lifecycle.LiveData;

import com.mqv.vmess.activity.viewmodel.AbstractMainViewModel;
import com.mqv.vmess.data.repository.FriendRequestRepository;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.repository.UserRepository;
import com.mqv.vmess.ui.data.People;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PeopleListFragmentViewModel extends AbstractMainViewModel {

    @Inject
    public PeopleListFragmentViewModel(UserRepository userRepository,
                                       FriendRequestRepository friendRequestRepository,
                                       PeopleRepository peopleRepository,
                                       NotificationRepository notificationRepository) {
        super(userRepository, friendRequestRepository, peopleRepository, notificationRepository);
    }

    @Override
    public void onRefresh() {

    }

    public LiveData<List<People>> getActivePeopleListSafe(){
        return getActivePeopleList();
    }
}
