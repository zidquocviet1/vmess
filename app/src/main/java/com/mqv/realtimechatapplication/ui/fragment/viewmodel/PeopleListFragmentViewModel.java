package com.mqv.realtimechatapplication.ui.fragment.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.mqv.realtimechatapplication.activity.viewmodel.AbstractMainViewModel;
import com.mqv.realtimechatapplication.activity.viewmodel.MainViewModel;
import com.mqv.realtimechatapplication.data.repository.FriendRequestRepository;
import com.mqv.realtimechatapplication.data.repository.PeopleRepository;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.ui.data.People;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PeopleListFragmentViewModel extends AbstractMainViewModel {

    @Inject
    public PeopleListFragmentViewModel(UserRepository userRepository,
                                       FriendRequestRepository friendRequestRepository,
                                       PeopleRepository peopleRepository) {
        super(userRepository, friendRequestRepository, peopleRepository);
    }

    @Override
    public void onRefresh() {

    }

    public LiveData<List<People>> getActivePeopleListSafe(){
        return getActivePeopleList();
    }
}
