package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.data.repository.UserRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class AccountSettingViewModel extends CurrentUserViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Boolean> signOutStatus = new MutableLiveData<>();

    @Inject
    public AccountSettingViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<Boolean> getSignOutStatus() {
        return signOutStatus;
    }

    public void signOut(String uid) {
        cd.add(userRepository.signOutHistoryUser(uid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> signOutStatus.setValue(true),
                        t -> signOutStatus.setValue(false)));
    }
}
