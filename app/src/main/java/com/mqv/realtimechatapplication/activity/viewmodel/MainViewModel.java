package com.mqv.realtimechatapplication.activity.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.data.repository.UserRepository;
import com.mqv.realtimechatapplication.data.result.Result;
import com.mqv.realtimechatapplication.network.model.User;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class MainViewModel extends CurrentUserViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Result<User>> remoteUser = new MutableLiveData<>();

    @Inject
    public MainViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;

        loadFirebaseUser();
        loadRemoteUserUsingNBR(null);
    }

    public LiveData<Result<User>> getRemoteUser() {
        return remoteUser;
    }

    public void loadRemoteUserUsingNBR(@Nullable User remoteUser) {
        var user = firebaseUser.getValue();
        if (user != null) {
            var uid = remoteUser != null ? remoteUser.getUid() : user.getUid();

            cd.add(userRepository.fetchUserUsingNBS(remoteUser, user)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(listUser -> {
                        var targetUser = listUser.stream()
                                .filter(u -> u.getUid().equals(uid))
                                .findAny()
                                .orElse(null);
                        this.remoteUser.setValue(Result.Success(targetUser));
                    }, t -> this.remoteUser.setValue(Result.Fail(R.string.error_connect_server_fail))));
        }
    }
}
