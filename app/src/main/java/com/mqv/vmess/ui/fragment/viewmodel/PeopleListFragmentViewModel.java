package com.mqv.vmess.ui.fragment.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.mqv.vmess.activity.viewmodel.AbstractMainViewModel;
import com.mqv.vmess.data.repository.FriendRequestRepository;
import com.mqv.vmess.data.repository.NotificationRepository;
import com.mqv.vmess.data.repository.PeopleRepository;
import com.mqv.vmess.data.repository.UserRepository;
import com.mqv.vmess.ui.data.People;
import com.mqv.vmess.util.LiveDataUtil;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

        loadAllPeople();
    }

    @Override
    public void onRefresh() {

    }

    public LiveData<List<People>> getActivePeopleListSafe(){
        return Transformations.map(LiveDataUtil.zip(getListPeople(), getPresenceUserList()), pair -> {
            if (pair != null) {
                List<People> people = pair.first;
                List<String> activeUserId = pair.second;

                return people.stream()
                             .filter(p -> activeUserId.contains(p.getUid()))
                             .collect(Collectors.toList());
            }
            return Collections.emptyList();
        });
    }
}
